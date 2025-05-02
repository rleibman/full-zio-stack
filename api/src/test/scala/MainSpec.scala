/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import Main.Environment
import zio.*
import zio.http.*
import zio.internal.stacktracer.Tracer
import zio.test.Assertion.*
import zio.test.{ZIOSpec, *}

object MainSpec extends ZIOSpec[Main.Environment] {

  override val bootstrap = EnvironmentBuilder.mock

  override def spec =
    suite("http")(
      test("should be ok") {
        val req = URL.decode("/text").map(s => Request(url = s)).getOrElse(Request())
        for {
          app <- Main.zapp
          one <- app(req)
        } yield assertTrue(one.status == Status.Ok)
      },
      test("Running two tests against the same app") {
        val req = URL.decode("/text").map(s => Request(url = s)).getOrElse(Request())

        for {
          app <- Main.zapp
          one <- app(req)
          two <- app(req)
        } yield assert(one.status)(equalTo(Status.Ok)) && assert(two.status)(equalTo(Status.Ok))
      }
    )

}
