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

import caliban.{GraphiQLHandler, QuickAdapter}
import net.leibman.fullziostack.config.HttpConfig
import net.leibman.fullziostack.graphql.{FullZIOStackApi, FullZIOStackService}
import zio.*
import zio.http.*

import java.nio.file.{Files, Path as FilePath}

object ApiRoutes {

  /** GraphQL at /api/graphql, GraphiQL at /api/graphiql, /health, and the client's static files. */
  def routes(http: HttpConfig): Task[Routes[FullZIOStackService, Response]] =
    FullZIOStackApi.api.interpreter.map { interpreter =>
      Routes(
        Method.ANY / "api" / "graphql"  -> QuickAdapter(interpreter).handlers.api,
        Method.GET / "api" / "graphiql" -> GraphiQLHandler.handler(apiPath = "/api/graphql", wsPath = None),
        Method.GET / "health"           -> Handler.text("ok")
      ) ++ staticFiles(FilePath.of(http.staticContentDir).nn)
    }

  /** Serves files from `directory`. Paths that aren't files get index.html, so client-side routes survive a reload. Nothing outside `directory` is
    * ever served.
    */
  private def staticFiles(directory: FilePath): Routes[Any, Response] = {
    val root = directory.toAbsolutePath.nn.normalize().nn
    Routes(
      Method.GET / trailing -> handler {
        (
          path: Path,
          _:    Request
        ) =>
          val requested = root.resolve(path.segments.mkString("/")).nn.normalize().nn
          val file = if (requested.startsWith(root) && Files.isRegularFile(requested)) requested else root.resolve("index.html").nn
          if (!file.startsWith(root) || !Files.isRegularFile(file)) ZIO.succeed(Response.notFound)
          else {
            val extension = file.getFileName.toString.split('.').lastOption.getOrElse("")
            val contentType = MediaType.forFileExtension(extension).getOrElse(MediaType.application.`octet-stream`)
            Body.fromFile(file.toFile.nn).map(body => Response(body = body, headers = Headers(Header.ContentType(contentType))))
          }
      }
    )
  }

}
