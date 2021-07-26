/*
 * Copyright (c) 2019 Roberto Leibman -- All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
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
    case Method.GET -> Root                 => Response.http(content = file("index.html"))
    case Method.GET -> Root / "text"        => Response.text("Hello World!")
    case Method.GET -> Root / somethingElse => Response.http(content = file(somethingElse))
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = Server.start(port, app).exitCode
}
