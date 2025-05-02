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

import caliban.*
import caliban.schema.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import caliban.wrappers.Wrappers.*
import graphql.FullZIOStackService
import model.*
import zio.*

import scala.language.postfixOps

object FullZIOStackApi {

  case class Queries(
    @GQLDescription("//TODO document this")
    all: URIO[FullZIOStackService, List[ModelObject]],
    @GQLDescription("//TODO document this")
    search: URIO[FullZIOStackService, List[ModelObject]],
    @GQLDescription("//TODO document this")
    get: ModelObjectId => URIO[FullZIOStackService, Option[ModelObject]]
  )

  case class Mutations(
    @GQLDescription("//TODO document this")
    delete: ModelObjectId => URIO[FullZIOStackService, Boolean],
    @GQLDescription("//TODO document this")
    upsert: ModelObject => URIO[FullZIOStackService, ModelObject]
  )

  given Schema[FullZIOStackService, ModelObjectId] = intSchema.contramap[ModelObjectId](_.asInt)
  given Schema[FullZIOStackService, ModelObjectType] = stringSchema.contramap[ModelObjectType](_.toString)
  given Schema[FullZIOStackService, ModelObject] = Schema.gen[FullZIOStackService, ModelObject]
  given ArgBuilder[ModelObjectId] = ArgBuilder.int.map[ModelObjectId](ModelObjectId.apply)
  given ArgBuilder[ModelObjectType] = ArgBuilder.string.map[ModelObjectType](ModelObjectType.valueOf)
  given Schema[FullZIOStackService, Queries] = Schema.gen[FullZIOStackService, Queries]
  given Schema[FullZIOStackService, Mutations] = Schema.gen[FullZIOStackService, Mutations]

  lazy val api: GraphQL[Console & Clock & FullZIOStackService] = graphQL(
    RootResolver(
      Queries(
        all = FullZIOStackService.all.map(_.toList),
        search = FullZIOStackService.search.map(_.toList),
        get = id => FullZIOStackService.get(id)
      ),
      Mutations(
        delete = id => FullZIOStackService.delete(id),
        upsert = modelObject => FullZIOStackService.upsert(modelObject)
      )
    )
  ) @@ maxFields(200) // query analyzer that limit query fields
    @@ maxDepth(30) // query analyzer that limit query depth
    @@ timeout(3 seconds) // wrapper that fails slow queries
    @@ printSlowQueries(500 millis) // wrapper that logs slow queries
    @@ printErrors // wrapper that logs errors

}
