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

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.HTMLElement
import net.leibman.fullziostack.semanticUiReact.components.*

object SampleForm {

  case class Props(name: String)

  case class State(
    name: String,
    dlg:  Boolean = true
  )

  class Backend($ : BackendScope[Props, State]) {

    def render(
      props: Props,
      state: State
    ): VdomElement =
      Modal.open(state.dlg)(
        ModalHeader("Results"),
        ModalContent(
          s"The name is ${state.name}",
          ModalActions(
            Button.onClick(
              (
                _,
                _
              ) => $.modState(_.copy(dlg = false))
            )("Close")
          )
        )
      )
//      Form(
//        FormField(
//          Label("First Name"),
//          Input.value(state.name).onChange { case (_, value) => $.modState(s => s.copy(name = value.value.asInstanceOf[String])) }
//        ),
//        Button.onClick((_, _) => $.modState(_.copy(dlg = true)))("Submit"),
//      ),
//      s"${state.name}")

  }

  private val component = ScalaComponent
    .builder[Props]("SampleForm")
    .initialStateFromProps(p => State(p.name))
    .renderBackend[Backend]
    .build

  def apply(name: String): Unmounted[Props, State, Backend] = component(Props(name))

}
