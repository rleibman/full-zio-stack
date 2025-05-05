import zio.http.endpoint.openapi.OpenAPI
import zio.http.gen.openapi.EndpointGen
import zio.http.gen.scala.CodeGen
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.File
import java.nio.file.Paths
import scala.io.Source

object DoCodeGen extends ZIOAppDefault {

  override def run = {
    for {
      _ <- ZIO.succeed(println("Generating code..."))
      api <- ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(File("codeGen/src/main/openapi/simple.json"))))(f => ZIO.attempt(f.close()).orDie) {
        source =>
          ZIO.attempt(
            OpenAPI.fromJson(
              source.getLines().mkString("\n")
            )
          )
      }
    } yield {
      println(api)
      val files = CodeGen.writeFiles(
        EndpointGen.fromOpenAPI(api.toOption.get),
        basePath = Paths.get("api/src/main/scala/apiClient"),
        basePackage = "apiClient",
        scalafmtPath = None
      )
      files
    }
  }

}
