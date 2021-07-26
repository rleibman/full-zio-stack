/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajdemo

import japgolly.scalajs.react.ScalaFnComponent
import japgolly.scalajs.react.component.ScalaFn.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object ScalaJSDemo {

  private val component = ScalaFnComponent[Unit](_ => <.div(SampleForm("Roberto")))

  def apply(): Unmounted[Unit] = component()
}
