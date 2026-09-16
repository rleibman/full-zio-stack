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

import caliban.interop.tapir.HttpInterpreter
import caliban.{CalibanError, Http4sAdapter}
import cats.syntax.all.*
import fs2.io.file.{Files as Fs2Files, Path as Fs2Path}
import net.leibman.fullziostack.config.HttpConfig
import net.leibman.fullziostack.graphql.ApiDefinition
import net.leibman.fullziostack.graphql.ApiDefinition.ApiEnvironment
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import zio.*
import zio.interop.catz.*

import java.nio.file.{Files, Path as FilePath}

object ApiRoutes {

  /** The effect every route runs in: the GraphQL routes need the service, so the others run there too. */
  type F[A] = RIO[ApiEnvironment, A]

  // fs2 3.7 deprecated deriving this from Async, which -Werror turns into an error.
  private given Fs2Files[F] = Fs2Files.forAsync[F]

  private val dsl = Http4sDsl[F]
  import dsl.*

  /** GraphQL at /api/graphql, GraphiQL at /api/graphiql, /health, and the client's static files. */
  def routes(http: HttpConfig): ZIO[ApiEnvironment, Throwable, HttpApp[F]] =
    ApiDefinition.api.interpreter.map { interpreter =>
      Router[F](
        "/api/graphql"  -> Http4sAdapter.makeHttpService[ApiEnvironment, CalibanError](HttpInterpreter(interpreter)),
        "/api/graphiql" -> Http4sAdapter.makeGraphiqlService[F]("/api/graphql"),
        "/"             -> (health <+> staticFiles(FilePath.of(http.staticContentDir).nn))
      ).orNotFound
    }

  private val health: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "health" => Ok("ok") }

  /** Serves files from `directory`. Paths that aren't files get index.html, so client-side routes survive a reload. Nothing outside `directory` is
    * ever served.
    */
  private def staticFiles(directory: FilePath): HttpRoutes[F] = {
    val root = directory.toAbsolutePath.nn.normalize().nn
    HttpRoutes.of[F] { case request @ GET -> path =>
      val requested = root.resolve(path.segments.map(_.decoded()).mkString("/")).nn.normalize().nn
      val file = if (requested.startsWith(root) && Files.isRegularFile(requested)) requested else root.resolve("index.html").nn
      if (!file.startsWith(root) || !Files.isRegularFile(file)) NotFound()
      else StaticFile.fromPath(Fs2Path.fromNioPath(file), Some(request)).getOrElseF(NotFound())
    }
  }

}
