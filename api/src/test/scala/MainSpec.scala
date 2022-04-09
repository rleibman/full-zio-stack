/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import Main.Environment
import config.{ConfigurationService, ConfigurationServiceLive}
import db.{MockDataServices, ModelObjectDataService}
import zhttp.http.*
import zio.*
import zio.internal.stacktracer.Tracer
import zio.logging.LogAnnotation
import zio.test.Assertion.*
import zio.test.{ZIOSpec, ZSpec, *}

object MainSpec extends ZIOSpec[TestEnvironment with Main.Environment] {

  implicit val trace: zio.ZTraceElement = Tracer.newTrace

  override val layer: ULayer[TestEnvironment & Main.Environment] =
    ZLayer.make[TestEnvironment & Main.Environment](
      ConfigurationServiceLive.layer,
      MockDataServices.modelObjectDataServices,
      zio.ZEnv.live,
      TestEnvironment.live,
      Scope.default
    )

  override def spec: ZSpec[TestEnvironment & Main.Environment & Scope, Any] =
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
