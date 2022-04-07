/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package graphql

import caliban.GraphQL.graphQL
import caliban.{GraphQL, RootResolver}
import caliban.schema.Annotations.{GQLDeprecated, GQLDescription}
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import db.ModelObjectDataService
import graphql.FullZIOStackService.FullZIOStackService
import model.*
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

  given Schema[Any, ModelObjectId] with {

    Schema.intSchema.contramap[ModelObjectId](_.asInt)

  }
  given Schema[Any, ModelObjectType] with {

    Schema.stringSchema.contramap[ModelObjectType](_.toString)

  }
  given Schema[Any, ModelObject] with {

    Schema.gen[Any, ModelObject]

  }
//  given Schema[Any, Queries] with {
//    Schema.gen[Any, Queries]
//  }
//  given Schema[Any, Mutations] with {
//    Schema.gen[Any, Mutations]
//  }

  val api: GraphQL[Console with Clock with FullZIOStackService] = graphQL(
    RootResolver(
      Queries(
        all = FullZIOStackService.all.orDie,
        search = FullZIOStackService.search.orDie,
        get = id => FullZIOStackService.get(id).orDie
      ),
      Mutations(
        delete = id => FullZIOStackService.delete(id).orDie,
        upsert = modelObject => FullZIOStackService.upsert(modelObject).orDie
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
