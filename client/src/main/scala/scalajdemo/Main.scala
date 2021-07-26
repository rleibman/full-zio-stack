/*
 * Copyright (c) 2019 Roberto Leibman -- All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
 */

package scalajdemo

import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit = ScalaJSDemo().renderIntoDOM(dom.document.getElementById("container"))
}
