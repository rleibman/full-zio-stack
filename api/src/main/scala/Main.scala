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

import config.ConfigurationService
import db.{DBIO, DataServiceException, ModelObjectDataService}
import zio.*
import zio.http.*

import java.io.{PrintWriter, StringWriter}

object Main extends ZIOApp {

  override type Environment = ConfigurationService & ModelObjectDataService[DBIO]
  override val environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]
  override def bootstrap:      ULayer[ConfigurationService & ModelObjectDataService[DBIO]] = EnvironmentBuilder.live

  def mapError(original: Cause[Throwable]): UIO[Response] = {
    lazy val contentTypeJson: Headers = Headers(Header.ContentType(MediaType.application.json).untyped)

    val squashed = original.squash
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    squashed.printStackTrace(pw)

    val body = "Error in DMScreen"
    // We really don't want details

    //    {
    //      s"""{
    //      "exceptionMessage": ${squashed.getMessage},
    //      "stackTrace": ${sw.toString}
    //    }"""
    //    }
    val status = squashed match {
      case e: DataServiceException if e.isTransient => Status.BadGateway
      case _ => Status.InternalServerError
    }
    ZIO
      .logErrorCause("Error in DMScreen", original).as(
        Response.apply(body = Body.fromString(body), status = status, headers = contentTypeJson)
      )
  }

  lazy val zapp = for {
    _           <- ZIO.log("Initializing Routes")
    fileRoutes  <- AllRoutes.fileRoutes
    modelRoutes <- AllRoutes.modelRoutes
  } yield (modelRoutes ++ fileRoutes)
    .handleErrorCauseZIO(mapError)

  override def run = {
    // Configure thread count using CLI
    for {
      config <- ZIO.serviceWithZIO[ConfigurationService](_.appConfig)
      app    <- zapp
      _      <- ZIO.logInfo(s"Starting application with config $config")
      server <- {
        val serverConfig = ZLayer.succeed(
          Server.Config.default
            .binding(config.http.hostName, config.http.port)
        )

        Server
          .serve(app)
          .zipLeft(ZIO.logDebug(s"Server Started on ${config.http.port}"))
          .tapErrorCause(ZIO.logErrorCause(s"Server on port ${config.http.port} has unexpectedly stopped", _))
          .provideSome[Environment](serverConfig, Server.live)
          .foldCauseZIO(
            cause => ZIO.logErrorCause("err when booting server", cause).exitCode,
            _ => ZIO.logError("app quit unexpectedly...").exitCode
          )
      }
    } yield server
  }

}
