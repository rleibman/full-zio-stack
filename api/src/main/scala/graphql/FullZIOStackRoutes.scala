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

package graphql

import caliban.{CalibanError, GraphiQLHandler, QuickAdapter}
import db.ModelObjectDataService
import zio.http.*
import zio.{Clock, Console, IO, ZIO}

object FullZIOStackRoutes {

  def api =
    for {
      interpreter <- FullZIOStackApi.api.interpreter
    } yield {
      Routes(
        Method.ANY / "api" ->
          QuickAdapter(interpreter).handlers.api,
        Method.ANY / "api" / "graphiql" ->
          GraphiQLHandler.handler(apiPath = "/api/dnd5e"),
        Method.POST / "api" / "upload" ->
          QuickAdapter(interpreter).handlers.upload,
        Method.GET / "unauth" / "schema" ->
          Handler.fromBody(Body.fromCharSequence(FullZIOStackApi.api.render))
      )
    }

}
