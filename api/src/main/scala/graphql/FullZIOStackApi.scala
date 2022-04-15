/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package graphql

import db.ModelObjectDataService
import graphql.FullZIOStackService.FullZIOStackService
import model.*
import caliban.GraphQL
import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.Annotations.{GQLDeprecated, GQLDescription}
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import zio.*
import zio.stream.ZStream

import scala.language.postfixOps

object FullZIOStackApi extends GenericSchema[FullZIOStackService] {

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
  given Schema[FullZIOStackService, ModelObject] = gen[FullZIOStackService, ModelObject]
  given ArgBuilder[ModelObjectId] = ArgBuilder.int.map[ModelObjectId](ModelObjectId.apply)
  given ArgBuilder[ModelObjectType] = ArgBuilder.string.map[ModelObjectType](ModelObjectType.valueOf)
  given Schema[FullZIOStackService, Queries] = gen[FullZIOStackService, Queries]
  given Schema[FullZIOStackService, Mutations] = gen[FullZIOStackService, Mutations]

  val api: GraphQL[Console & Clock & FullZIOStackService] = graphQL(
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
  ) @@
    maxFields(200) @@ // query analyzer that limit query fields
    maxDepth(30) @@ // query analyzer that limit query depth
    timeout(3 seconds) @@ // wrapper that fails slow queries
    printSlowQueries(500 millis) @@ // wrapper that logs slow queries
    printErrors @@ // wrapper that logs errors
    apolloTracing // wrapper for https://github.com/apollographql/apollo-tracing

}
