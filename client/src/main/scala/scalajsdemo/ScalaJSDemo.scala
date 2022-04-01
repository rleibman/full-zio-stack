/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajsdemo

import japgolly.scalajs.react.ScalaFnComponent
import japgolly.scalajs.react.component.ScalaFn.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object ScalaJSDemo {

  private val component = ScalaFnComponent[Unit](_ => <.div("Hello!", SampleForm("Roberto")))

  def apply(session: Option[Session]): Unmounted[Unit] = component()

}
