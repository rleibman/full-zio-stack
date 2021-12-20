/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import zhttp.http._
import zhttp.service._
import zio._
import zio.stream._
import java.nio.file.{Paths => JPaths}

object Main extends zio.App {
  val rootDir = "/Volumes/Personal/projects/full-zio-stack/dist/"
  val port = 8090

  def file(fileName: String) =
    HttpData.fromStream {
      ZStream.fromFile(JPaths.get(s"$rootDir$fileName"))
    }

  val app = Http.collect[Request] {
    case Method.GET -> !!                 => Response(data = file("index.html"))
    case Method.GET -> !! / "text"        => Response.text("Hello World!")
    case Method.GET -> !! / somethingElse => Response(data = file(somethingElse))
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = Server.start(port, app).exitCode
}
