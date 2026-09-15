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

import net.leibman.fullziostack.config.{AppConfig, HttpConfig}
import net.leibman.fullziostack.db.DatabaseConfig
import net.leibman.fullziostack.server.AppLayers.AppEnvironment
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.{Files, Path}

/** What every HTTP server must do. Each server module has a ServerSpec that extends this and says how to start it.
  *
  * The server runs on a random port against the in-memory data service, and is exercised over real HTTP with the JDK client, so the same assertions
  * hold for any server implementation.
  */
abstract class ServerContractSpec extends ZIOSpecDefault {

  /** Starts the server on `AppConfig.http` (port 0 means any free port) and returns the port it listens on. The server stops when the scope closes.
    */
  def startServer: ZIO[AppEnvironment & Scope, Throwable, Int]

  case class Response(
    status: Int,
    body:   String
  )

  case class Server(port: Int) {

    private val client = HttpClient.newHttpClient().nn

    private def send(request: HttpRequest.Builder): Task[Response] =
      ZIO
        .fromCompletableFuture(client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString()).nn)
        .map(response => Response(response.statusCode(), response.body().nn))

    def get(path: String): Task[Response] = send(HttpRequest.newBuilder(URI.create(s"http://127.0.0.1:$port$path")).nn.GET().nn)

    /** Runs a GraphQL operation and returns the parsed response body. */
    def graphql(query: String): Task[Json] =
      send(
        HttpRequest
          .newBuilder(URI.create(s"http://127.0.0.1:$port/api/graphql"))
          .nn
          .header("Content-Type", "application/json")
          .nn
          .POST(HttpRequest.BodyPublishers.ofString(Json.Obj("query" -> Json.Str(query)).toJson))
          .nn
      ).flatMap(response => ZIO.fromEither(response.body.fromJson[Json]).mapError(Exception(_)))

  }

  /** Walks a JSON path of object fields and array indexes, e.g. at(json, "data", "items", 0, "name"). */
  def at(
    json: Json,
    path: (String | Int)*
  ): Option[Json] =
    path.foldLeft(Option(json)) {
      case (Some(Json.Obj(fields)), field: String) => fields.collectFirst { case (`field`, value) => value }
      case (Some(Json.Arr(items)), index: Int)     => items.lift(index)
      case _                                       => None
    }

  private val staticFiles: ZLayer[Any, Throwable, Path] = ZLayer.scoped {
    // Explicit Path types: izumi-reflect can't build a Tag for the flexible `Path | Null` of a Java call.
    ZIO.acquireRelease(ZIO.attemptBlocking[Path] {
      val root:   Path = Files.createTempDirectory("server-contract").nn
      val static: Path = Files.createDirectories(root.resolve("static")).nn
      Files.writeString(static.resolve("index.html"), "<html>the app</html>")
      Files.writeString(static.resolve("app.js"), "console.log('app')")
      // Outside the static directory: must never be served.
      Files.writeString(root.resolve("secret.txt"), "secret")
      static
    })(static => ZIO.attemptBlocking(deleteRecursively(static.getParent.nn)).orDie)
  }

  private def deleteRecursively(path: Path): Unit = {
    if (Files.isDirectory(path)) Files.list(path).nn.forEach(p => deleteRecursively(p.nn))
    Files.delete(path)
  }

  private val server: ZLayer[Any, Throwable, Server] =
    staticFiles.flatMap { static =>
      val config = AppConfig(
        http = HttpConfig(host = "127.0.0.1", port = 0, staticContentDir = static.get.toString),
        db = DatabaseConfig(url = "unused", migrationsLocation = "unused")
      )
      AppLayers.mock(config) >>> ZLayer.scoped(startServer.map(Server.apply))
    }

  private val upsert =
    """mutation { upsertModelObject(modelObject: {id: -1, name: "From the contract", description: "d", modelObjectType: type2,
      |  deleted: false, created: "1970-01-01T00:00:00Z", lastUpdated: "1970-01-01T00:00:00Z"}) { id name } }""".stripMargin

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("HTTP server contract")(
      test("/health answers ok") {
        ZIO.serviceWithZIO[Server](_.get("/health")).map(r => assertTrue(r.status == 200, r.body == "ok"))
      },
      test("a GraphQL mutation saves an object that queries then find") {
        for {
          server <- ZIO.service[Server]
          saved  <- server.graphql(upsert)
          id     <- ZIO.fromOption(at(saved, "data", "upsertModelObject", "id").collect { case Json.Num(n) => n.intValue })
          read   <- server.graphql(s"{ modelObject(id: $id) { name } }")
          found  <- server.graphql("""{ modelObjects(search: {text: "contract", includeDeleted: false, offset: 0, limit: 10}) { total } }""")
        } yield assertTrue(
          at(read, "data", "modelObject", "name").contains(Json.Str("From the contract")),
          at(found, "data", "modelObjects", "total").contains(Json.Num(1))
        )
      },
      test("invalid input fails with the BAD_USER_INPUT error code") {
        ZIO
          .serviceWithZIO[Server](_.graphql(upsert.replace("From the contract", " ")))
          .map(response => assertTrue(at(response, "errors", 0, "extensions", "code").contains(Json.Str("BAD_USER_INPUT"))))
      },
      test("the version query answers") {
        ZIO
          .serviceWithZIO[Server](_.graphql("{ version }"))
          .map(response => assertTrue(at(response, "data", "version").exists(_.isInstanceOf[Json.Str])))
      },
      test("GraphiQL is served") {
        ZIO.serviceWithZIO[Server](_.get("/api/graphiql")).map(r => assertTrue(r.status == 200, r.body.contains("graphiql")))
      },
      test("static files are served, and unknown paths fall back to index.html for client-side routing") {
        for {
          server <- ZIO.service[Server]
          index  <- server.get("/")
          js     <- server.get("/app.js")
          route  <- server.get("/some/client/route")
        } yield assertTrue(
          index.status == 200,
          index.body == "<html>the app</html>",
          js.body == "console.log('app')",
          route.status == 200,
          route.body == "<html>the app</html>"
        )
      },
      test("nothing outside the static directory is served") {
        for {
          server  <- ZIO.service[Server]
          plain   <- server.get("/../secret.txt")
          encoded <- server.get("/%2e%2e/secret.txt")
        } yield assertTrue(!plain.body.contains("secret"), !encoded.body.contains("secret"))
      }
    ).provideShared(server) @@ TestAspect.sequential @@ TestAspect.withLiveClock

}
