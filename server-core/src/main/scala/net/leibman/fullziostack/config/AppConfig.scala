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

package net.leibman.fullziostack.config

import com.typesafe.config.ConfigFactory
import net.leibman.fullziostack.db.DatabaseConfig
import net.leibman.fullziostack.telemetry.TelemetryConfig
import zio.*
import zio.config.magnolia.DeriveConfig
import zio.config.typesafe.*

case class HttpConfig(
  host: String,
  port: Int,
  /** Directory the built client is served from, relative to the working directory: `dist` (production build) or `debugDist` (development build).
    */
  staticContentDir: String
)

case class AppConfig(
  http:      HttpConfig,
  db:        DatabaseConfig,
  telemetry: TelemetryConfig,
  /** Only used by projects generated with AI; harmless otherwise. */
  ai: AiConfig = AiConfig(),
  /** Only used by projects generated with authentication; harmless otherwise. */
  auth: AuthSettings = AuthSettings()
)

object AppConfig {

  /** Reads the `app` section of application.conf. Override the file with `-Dconfig.file=...`, or single values with the environment variables named
    * in application.conf.
    */
  val live: TaskLayer[AppConfig] = ZLayer.fromZIO(
    ZIO
      .attempt(ConfigFactory.load().nn.getConfig("app").nn)
      .flatMap(config => TypesafeConfigProvider.fromTypesafeConfig(config).load(DeriveConfig.derived[AppConfig].desc))
  )

  val database: URLayer[AppConfig, DatabaseConfig] = ZLayer.fromFunction((config: AppConfig) => config.db)

  val telemetry: URLayer[AppConfig, TelemetryConfig] = ZLayer.fromFunction((config: AppConfig) => config.telemetry)

  val ai: URLayer[AppConfig, AiConfig] = ZLayer.fromFunction((config: AppConfig) => config.ai)

  val auth: URLayer[AppConfig, AuthSettings] = ZLayer.fromFunction((config: AppConfig) => config.auth)

}
