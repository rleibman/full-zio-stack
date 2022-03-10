/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package scalajsdemo

import org.scalajs.dom
import zio.*

object Session {

  def loadSession: Task[Session] = ???

}

trait Session {}

object Main {

  type Environment = Session

  def main(args: Array[String]): Unit = {
    val session = new Session {}
    val sessionLayer = ZLayer.fromZIO(ZIO.succeed(session))

    ScalaJSDemo(Some(session)).renderIntoDOM(dom.document.getElementById("container"))
    zio.Runtime.default.unsafeRunAsync(
      (for {
        session <- ZIO.service[Session]
      } yield ScalaJSDemo(Some(session)).renderIntoDOM(dom.document.getElementById("container"))).provideCustomLayer(sessionLayer)
    )

  }

}
