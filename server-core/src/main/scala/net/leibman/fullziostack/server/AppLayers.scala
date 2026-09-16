/*
 * Copyright (c) 2024 Roberto Leibman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.leibman.fullziostack.server

import net.leibman.fullziostack.config.AppConfig
import net.leibman.fullziostack.db.{DataSources, MockRepository, ZIORepository}
import net.leibman.fullziostack.graphql.ApiDefinition
import net.leibman.fullziostack.telemetry.Telemetry
import zio.telemetry.opentelemetry.tracing.Tracing
import zio.*
import zio.logging.backend.SLF4J

import javax.sql.DataSource

/** How the application is wired, independent of the HTTP server. */
object AppLayers {

  /** Everything the HTTP servers need. */
  type AppEnvironment = AppConfig & ApiDefinition.ApiEnvironment & AuthModule.Env

  /** Config → DataSource (pooled, migrated, closed on shutdown) → repository, plus tracing and whatever the variants
    * add (the AI service, in projects generated with AI; zio-auth, in projects generated with authentication). The
    * DataSource is part of the result because authentication keeps its users in the same database.
    */
  private val base: TaskLayer[AppConfig & ZIORepository & Tracing & DataSource] = ZLayer.make[AppConfig & ZIORepository & Tracing & DataSource](
    AppConfig.live,
    AppConfig.database,
    DataSources.live,
    ZIORepository.live,
    AppConfig.telemetry,
    Telemetry.live
  )

  val live: TaskLayer[AppEnvironment] = base ++ (base >>> ApiDefinition.extraLayers) ++ (base >>> AuthModule.layers)

  /** The same, over in-memory data: for tests, or for running without a database. */
  def mock(config: AppConfig): TaskLayer[AppEnvironment] =
    ZLayer.succeed(config) ++ MockRepository.live ++ (ZLayer.succeed(config.telemetry) >>> Telemetry.live) ++
      (ZLayer.succeed(config) >>> ApiDefinition.extraLayers) ++ (ZLayer.succeed(config) >>> AuthModule.mockLayers)

  /** Sends ZIO's log output through SLF4J (configured by logback.xml). */
  val logging: ZLayer[Any, Nothing, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

}
