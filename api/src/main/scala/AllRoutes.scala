import zio.*
import zio.http.*

object AllRoutes {

  val fileRoutes = ZIO.succeed {
    Routes.empty
  }

  /* case Method.GET -> !! => ZIO.succeed(Response(data = file("index.html")))
   * case Method.GET -> !! / "text" => ZIO.succeed(Response.text("Hello World!"))
   * case Method.GET -> !! / somethingElse => ZIO.succeed(Response(data = file(somethingElse)))
   */

  val modelRoutes = ZIO.succeed {
    Routes.empty
  }
  /*
      case Method.GET -> !! / "modelObject" / id        => db.get(ModelObjectId(id.toInt)).map(o => Response.json(o.toJson))
      case Method.GET -> !! / "modelObjects"            => db.search().map(o => Response.json(o.toJson))
      case Method.GET -> !! / "modelObjects" / "search" => db.search().map(o => Response.json(o.toJson))
      case Method.DELETE -> !! / "modelObject" / id     => db.delete(ModelObjectId(id.toInt), softDelete = true).map(o => Response.json(o.toJson))
      case Method.POST -> !! / "modelObject" | Method.PUT -> !! / "modelObject" =>
        for {
          str      <- request.bodyAsString
          obj      <- ZIO.fromEither(str.fromJson[ModelObject]).mapError(new Exception(_))
          upserted <- db.upsert(obj)
        } yield Response.json(upserted.toJson)

  
   */

}
