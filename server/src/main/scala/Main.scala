/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import zhttp.http.*
import zhttp.service.*
import zio.*
import zio.stream.*

import java.nio.file
import java.nio.file.Paths as JPaths

object Main extends zio.App {
  val rootDir = "/Volumes/Personal/projects/full-zio-stack/dist/"
  val port = 8090

  type Environment = ZEnv

  def file(fileName: String): HttpData[Environment, Throwable] =
    HttpData.fromStream {
      JPaths.get(s"$rootDir$fileName") match {
        case path: java.nio.file.Path => ZStream.fromFile(path)
        case null => throw new NullPointerException()
      }
    }

  val app: HttpApp[Environment, Throwable] = Http.collect[Request] {
    case Method.GET -> !!                 => Response(data = file("index.html"))
    case Method.GET -> !! / "text"        => Response.text("Hello World!")
    case Method.GET -> !! / somethingElse => Response(data = file(somethingElse))
  }

  override def run(args: List[String]): URIO[Environment, ExitCode] = Server.start(port, app).exitCode
}
