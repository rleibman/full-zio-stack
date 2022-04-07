/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import zhttp.http.*
import zio.logging.{LogAnnotation, Logging}
import zio.test.Assertion.*
import zio.test.*
import zio.{Has, ULayer}

object MainSpec extends ZIOSpecDefault {

  def spec: Spec[Any, TestFailure[Serializable], TestSuccess] =
    suite("http")(
      test("should be ok") {
        val req = URL.fromString("/text").map(s => Request(url = s)).getOrElse(Request())
        for {
          app <- Main.zapp
          one <- app(req)
        } yield assertTrue(one.status == Status.Ok)
      },
      test("Running two tests against the same app") {
        val req = URL.fromString("/text").map(s => Request(url = s)).getOrElse(Request())

        for {
          app <- Main.zapp
          one <- app(req)
          two <- app(req)
        } yield assert(one.status)(equalTo(Status.Ok)) && assert(two.status)(equalTo(Status.Ok))
      }
    )

}
