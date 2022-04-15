/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajsdemo

import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^.*
import model.*
import model.given
import net.leibman.fullziostack.semanticUiReact.components.*
import org.scalajs.dom.HTMLElement
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
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
