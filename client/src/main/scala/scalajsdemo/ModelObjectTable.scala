/*
 * Copyright (c) 2024 Roberto Leibman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package scalajsdemo

import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^.*
import model.*
import model.given
import net.leibman.fullziostack.semanticUiReact.components.*
import org.scalajs.dom.HTMLElement
import zio.http.*
import zio.*
import zio.json.*

import scala.concurrent.Future

extension [A](z: Task[A]) {

  def toAsyncCallback: AsyncCallback[A] = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits._
    def fut = zio.Runtime.default.unsafeRunToFuture(z)
    AsyncCallback.fromFuture(fut)
  }
  def toCallback(fn: A => Callback): Callback = {
    z.map(fn).toAsyncCallback.completeWith(_.get)
  }
  def toCallback: Callback = {
    toAsyncCallback.toCallback
  }

}

object ModelObjectTable {

  case class State(
    modelObjects: IndexedSeq[ModelObject]
  )

  class Backend($ : BackendScope[Unit, State]) {

    import scala.scalajs.concurrent.JSExecutionContext.Implicits._
    def init(): Callback = {
      val z: Task[IndexedSeq[ModelObject]] = (for {
        res    <- Client.request("/modelObjects")
        str    <- res.bodyAsString
        parsed <- ZIO.fromEither(str.fromJson[IndexedSeq[ModelObject]]).mapError(new Exception(_))
      } yield parsed).provideLayer(ChannelFactory.auto ++ EventLoopGroup.auto())

      z.toCallback(modelObjects => $.modState(s => s.copy(modelObjects = modelObjects)))
    }

    def render(
      state: State
    ): VdomElement = <.div()

  }

  private val component = ScalaComponent
    .builder[Unit]("ModelObjectTable")
    .initialState(State(IndexedSeq.empty))
    .renderBackend[Backend]
    .componentDidMount(_.backend.init())
    .build

  def apply(name: String): Unmounted[Unit, State, Backend] = component()

}
