/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import Main.Environment
import config.{ConfigurationService, ConfigurationServiceLive}
import db.{MockDataServices, ModelObjectDataService}
import zio.http.*
import zio.*
import zio.internal.stacktracer.Tracer
import zio.logging.LogAnnotation
import zio.test.Assertion.*
import zio.test.{ZIOSpec, *}

object MainSpec extends ZIOSpec[TestEnvironment with Main.Environment] {

  override val bootstrap: ZLayer[ZIOAppArgs with Scope, Any, TestEnvironment & Main.Environment] =
    ZLayer.make[TestEnvironment & Main.Environment](
      ConfigurationServiceLive.layer,
      MockDataServices.modelObjectDataServices,
      zio.ZEnv.live,
      TestEnvironment.live
    )

  override def spec =
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
