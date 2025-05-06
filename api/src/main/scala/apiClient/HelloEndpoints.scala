package apiClient

import zio.ZNothing
import zio.http.*
import zio.http.codec.*
import zio.http.endpoint.*
import zio.http.endpoint.AuthType.None
import zio.schema.{DeriveSchema, Schema}

object HelloEndpoints {

  // The types
  case class ResponseBody(
    message: Option[String]
  )

  // The Schemas
  given Schema[ResponseBody] = DeriveSchema.gen[ResponseBody]

  // The endpoints
  val getGreeting: Endpoint[Unit, Unit, ZNothing, ResponseBody, None] = Endpoint(Method.GET / "hello")
    .in[Unit]
    .out[ResponseBody](status = Status.Ok)

}
