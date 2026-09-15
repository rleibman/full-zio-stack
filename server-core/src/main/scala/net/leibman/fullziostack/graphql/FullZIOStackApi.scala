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

package net.leibman.fullziostack.graphql

import caliban.*
import caliban.CalibanError.ExecutionError
import caliban.schema.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.wrappers.Wrappers.*
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.server.BuildInfo
import zio.*

import scala.language.postfixOps

/** The GraphQL API. After changing it, run `calibanRender` to update the committed schema.graphql, which the client's generated code is built from
  * (SchemaSpec fails until you do).
  */
object FullZIOStackApi {

  type Op[A] = ZIO[FullZIOStackService, ExecutionError, A]

  /** Derives schemas for types whose fields need FullZIOStackService (Schema.auto only covers Any). */
  object ApiSchema extends GenericSchema[FullZIOStackService]
  import ApiSchema.auto.*

  case class ModelObjectArgs(id: ModelObjectId)

  case class ModelObjectsArgs(search: ModelObjectSearch)

  case class UpsertModelObjectArgs(modelObject: ModelObject)

  case class DeleteModelObjectArgs(
    id:         ModelObjectId,
    softDelete: Boolean
  )

  case class Queries(
    @GQLDescription("Model objects matching the search, one page at a time")
    modelObjects: ModelObjectsArgs => Op[Page[ModelObject]],
    @GQLDescription("A model object by id, including soft-deleted ones")
    modelObject: ModelObjectArgs => Op[Option[ModelObject]],
    @GQLDescription("The server's version")
    version: String
  )

  case class Mutations(
    @GQLDescription("Creates the model object when its id is -1, otherwise updates it. Returns what was saved.")
    upsertModelObject: UpsertModelObjectArgs => Op[ModelObject],
    @GQLDescription("Soft delete marks the object as deleted; hard delete removes it. False if there was nothing to delete.")
    deleteModelObject: DeleteModelObjectArgs => Op[Boolean]
  )

  given Schema[Any, ModelObjectId] = Schema.intSchema.contramap(_.value)
  given ArgBuilder[ModelObjectId] = ArgBuilder.int.map(ModelObjectId.apply)
  // Explicit, so graphQL(...) below is typed for FullZIOStackService instead of inferring Any.
  given Schema[FullZIOStackService, Queries] = ApiSchema.gen[FullZIOStackService, Queries]
  given Schema[FullZIOStackService, Mutations] = ApiSchema.gen[FullZIOStackService, Mutations]

  lazy val api: GraphQL[FullZIOStackService] =
    graphQL(
      RootResolver(
        Queries(
          modelObjects = args => ZIO.serviceWithZIO[FullZIOStackService](_.modelObjects(args.search)),
          modelObject = args => ZIO.serviceWithZIO[FullZIOStackService](_.modelObject(args.id)),
          version = BuildInfo.version
        ),
        Mutations(
          upsertModelObject = args => ZIO.serviceWithZIO[FullZIOStackService](_.upsertModelObject(args.modelObject)),
          deleteModelObject = args => ZIO.serviceWithZIO[FullZIOStackService](_.deleteModelObject(args.id, args.softDelete))
        )
      )
    ) @@ maxFields(200) // query analyzer that limit query fields
      @@ maxDepth(30) // query analyzer that limit query depth
      @@ timeout(3 seconds) // wrapper that fails slow queries
      @@ printSlowQueries(500 millis) // wrapper that logs slow queries

}
