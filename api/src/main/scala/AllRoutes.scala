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
