/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajsdemo

import japgolly.scalajs.react.ScalaFnComponent
import japgolly.scalajs.react.component.ScalaFn.Unmounted
import japgolly.scalajs.react.util.Effect
import japgolly.scalajs.react.vdom.html_<^.*

object ZIOComponent {

  private val component = ScalaFnComponent[Unit](_ => <.div("Hello!"))

  def apply(): Unmounted[Unit] = component()

}
