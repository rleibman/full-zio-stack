package apiClient

import zio.cli.HelpDoc
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.cli.HttpCliApp
import zio.schema.{DeriveSchema, Schema}
import zio.{Chunk, Scope, Task, ZIO, ZIOAppDefault, ZLayer, ZNothing}

object Hello {

  import zio.http.*
  import zio.http.codec.*
  import zio.http.endpoint.*
  val getGreeting: Endpoint[Unit, Unit, ZNothing, GET.ResponseBody, None] = Endpoint(Method.GET / "hello")
    .in[Unit]
    .out[GET.ResponseBody](status = Status.Ok)

  object GET {

    case class ResponseBody(
      message: Option[String]
    )

    given Schema[ResponseBody] = DeriveSchema.gen[ResponseBody]

  }

}
