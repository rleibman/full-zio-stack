/*
 * Copyright (c) 2019 Roberto Leibman -- All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
 */

package scalajdemo

import japgolly.scalajs.react.ScalaFnComponent
import japgolly.scalajs.react.component.ScalaFn.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object ScalaJSDemo {

  private val component = ScalaFnComponent[Unit](_ => <.div(SampleForm("Roberto")))

  def apply(): Unmounted[Unit] = component()
}
