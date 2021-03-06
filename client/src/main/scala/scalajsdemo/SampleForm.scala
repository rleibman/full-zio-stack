/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
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
            Button.onClick((_, _) => $.modState(_.copy(dlg = false)))("Close")
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
