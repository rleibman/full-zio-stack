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

import com.comcast.ip4s.{Host, Port}
import fs2.io.net.Network
import net.leibman.fullziostack.config.AppConfig
import net.leibman.fullziostack.db.ZIORepository
import net.leibman.fullziostack.server.ApiRoutes.F
import net.leibman.fullziostack.server.AppLayers.AppEnvironment
import org.http4s.ember.server.EmberServerBuilder
import zio.*
import zio.interop.catz.*

/** The http4s (ember) server. */
object Main extends ZIOApp {

  override type Environment = AppEnvironment
  override val environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]
  override val bootstrap:      TaskLayer[AppEnvironment] = AppLayers.logging >>> AppLayers.live

  // fs2 3.7 deprecated deriving this from Async, which -Werror turns into an error.
  private given Network[F] = Network.forAsync[F]

  /** Starts the server and returns the port it listens on (useful when the configured port is 0). It stops when the scope closes.
    */
  val startServer: ZIO[AppEnvironment & Scope, Throwable, Int] =
    for {
      config <- ZIO.service[AppConfig]
      host   <- ZIO.fromOption(Host.fromString(config.http.host)).orElseFail(IllegalArgumentException(s"Bad host: ${config.http.host}"))
      port   <- ZIO.fromOption(Port.fromInt(config.http.port)).orElseFail(IllegalArgumentException(s"Bad port: ${config.http.port}"))
      app    <- ApiRoutes.routes(config.http)
      server <- EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withHttpApp(app)
        // On shutdown, wait at most this long for open connections (ember's default is 30 s; zio-http's is 10 s).
        .withShutdownTimeout(10.seconds.asFiniteDuration)
        .build
        .toScopedZIO
    } yield server.address.getPort

  override val run: ZIO[AppEnvironment & ZIOAppArgs & Scope, Any, Any] =
    for {
      config <- ZIO.service[AppConfig]
      port   <- startServer
      _      <- ZIO.logInfo(s"Listening on http://${config.http.host}:$port, serving ${config.http.staticContentDir}")
      _      <- ZIO.never
    } yield ()

}
