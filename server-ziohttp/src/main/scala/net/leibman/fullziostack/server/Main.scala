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
import net.leibman.fullziostack.server.AppLayers.AppEnvironment
import zio.*
import zio.http.*

/** The zio-http server. */
object Main extends ZIOApp {

  override type Environment = AppEnvironment
  override val environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]
  override val bootstrap:      TaskLayer[AppEnvironment] = AppLayers.logging >>> AppLayers.live

  /** Starts the server and returns the port it listens on (useful when the configured port is 0). It stops when the scope closes.
    */
  val startServer: ZIO[AppEnvironment & Scope, Throwable, Int] =
    for {
      config <- ZIO.service[AppConfig]
      routes <- ApiRoutes.routes(config.http)
      server <- (ZLayer.succeed(Server.Config.default.binding(config.http.host, config.http.port)) >>> Server.live).build
      port   <- Server.install(routes).provideSomeEnvironment[AppEnvironment](_ ++ server)
    } yield port

  override val run: ZIO[AppEnvironment & ZIOAppArgs & Scope, Any, Any] =
    for {
      config <- ZIO.service[AppConfig]
      port   <- startServer
      _      <- ZIO.logInfo(s"Listening on http://${config.http.host}:$port, serving ${config.http.staticContentDir}")
      _      <- ZIO.never
    } yield ()

}
