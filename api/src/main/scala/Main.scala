/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import config.*
import izumi.reflect.Tag
import zhttp.http.*
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.logging.backend.SLF4J
import zio.stream.*
import zio.{Console, ExitCode, RuntimeConfigAspect, UIO, ULayer, ZEnv, ZIO, ZIOApp, ZLayer}

import java.nio.file
import java.nio.file.Paths as JPaths

object Main extends ZIOApp {

  // Customize the application a bit
  override type Environment = ZEnv & ConfigurationService

  def tag: Tag[Environment] = Tag[Environment]

  override def hook: RuntimeConfigAspect = SLF4J.slf4j(zio.LogLevel.Debug)

  override val layer: ULayer[Environment] = ZLayer.make[ZEnv & ConfigurationService](ZEnv.live, ConfigurationServiceLive.layer)
  val rootDir = "/Volumes/Personal/projects/full-zio-stack/dist/"
  val port = 8090

  private def logRequest(req: Request): UIO[Unit] =
    (for {
      body <- req.bodyAsString
      _ <-
        ZIO.logInfo(s"${req.method.toString()} request to ${req.url.encode}: " + body)
    } yield ()).ignore

  private def file(fileName: String) =
    HttpData.fromStream {
      JPaths.get(s"$rootDir$fileName") match {
        case path: java.nio.file.Path => ZStream.fromPath(path)
        case null => throw new NullPointerException()
      }
    }

  private val fileRouteZIO: ZIO[Environment, Throwable, Http[Environment, Throwable, Request, Response]] =
    for {
      config <- ZIO.serviceWithZIO[ConfigurationService](_.config)
    } yield Http.collectZIO[Request] { request =>
      logRequest(request) *>
        (request match {
          case Method.GET -> !!                 => ZIO(Response(data = file("index.html")))
          case Method.GET -> !! / "text"        => ZIO(Response.text("Hello World!"))
          case Method.GET -> !! / somethingElse => ZIO(Response(data = file(somethingElse)))
        })
    }

  val zapp: ZIO[Environment, Throwable, Http[Environment, Throwable, Request, Response]] =
    for {
      _         <- ZIO.log("Initializing Routes")
      fileRoute <- fileRouteZIO
    } yield fileRoute

  def run: ZIO[Environment, Throwable, ExitCode] = {
    // Configure thread count using CLI
    (for {
      config <- ZIO.serviceWithZIO[ConfigurationService](_.config)
      app    <- zapp
      server <- {
        (
          Server.bind(config.host, config.port) ++ // Setup port
            Server.maxRequestSize(config.maxRequestSize) ++
            Server.app(app)
        ).make
          .use(start =>
            // Waiting for the server to start
            ZIO.logInfo(s"Server started on port ${start.port}") *>
              ZIO.never // Ensures the server doesn't die after printing
          )
          // TODO, the next is clearly wrong, we shouldn't need to provide the configuration layer. We need to figure out how to bubble up te cofig service
          .provideCustom(ConfigurationServiceLive.layer, ServerChannelFactory.auto, EventLoopGroup.auto(config.nettyThreads)).exitCode
      }
    } yield server).tapErrorCause(ZIO.logErrorCause(_))
  }

}
