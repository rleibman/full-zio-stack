/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajdemo

import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit = ScalaJSDemo().renderIntoDOM(dom.document.getElementById("container"))
}
