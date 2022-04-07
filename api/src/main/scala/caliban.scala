/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

//import caliban.GraphQL.graphQL
//import caliban.schema.Schema
//import caliban.*
//import db.*
//import model.*
//import zhttp.http.HttpApp
//import zio.*
//
//given Schema[ModelObjectDataService, ModelObjectId] with {
//  Schema.intSchema.contramap[ModelObjectId](_.asInt)
//}
//
//given Schema[ModelObjectDataService, ModelObjectType] with {
//  Schema.stringSchema.contramap[ModelObjectType](_.toString)
//}
//
//given Schema[Any, ModelObject] with {
//  ??? //Schema.stringSchema.contramap[ModelObjectType](_.toString)
//}
//
//case class ModelObjectQueries(
//  all:    ZIO[ModelObjectDataService, Throwable, List[ModelObject]],
//  search: ZIO[ModelObjectDataService, Throwable, List[ModelObject]],
//  get:    ModelObjectId => ZIO[ModelObjectDataService, Throwable, Option[ModelObject]]
//)
//
//case class ModelObjectMutations(
//  delete: ModelObjectId => ZIO[ModelObjectDataService, Throwable, Boolean],
//  upsert: ModelObject => ZIO[ModelObjectDataService, Throwable, ModelObject]
//)
//
////given Schema[Any, ModelObject] with {
////  ??? //Schema.stringSchema.contramap[ModelObjectType](_.toString)
////}
//
////given Schema[ModelObjectDataService, ModelObjectQueries] with {
////  Schema.gen[ModelObjectDataService, ModelObjectQueries]
////}
//
//val modelObjectApi: GraphQL[ModelObjectDataService] = graphQL(
//  RootResolver(
//    ModelObjectQueries(
//      all = for {
//        db      <- ZIO.service[ModelObjectDataService]
//        results <- db.search()
//      } yield results,
//      search = for {
//        db      <- ZIO.service[ModelObjectDataService]
//        results <- db.search()
//      } yield results,
//      get = { (id: ModelObjectId) =>
//        for {
//          db      <- ZIO.service[ModelObjectDataService]
//          results <- db.get(id)
//        } yield results
//      }
//    ),
//    ModelObjectMutations(
//      delete = { (id: ModelObjectId) =>
//        for {
//          db      <- ZIO.service[ModelObjectDataService]
//          results <- db.delete(id, softDelete = true)
//        } yield results
//      },
//      upsert = { (obj: ModelObject) =>
//        for {
//          db      <- ZIO.service[ModelObjectDataService]
//          results <- db.upsert(obj)
//        } yield results
//      }
//    )
//  )
//)
//
//val interpreter: IO[CalibanError.ValidationError, GraphQLInterpreter[ModelObjectDataService, CalibanError]] = modelObjectApi.interpreter
//
//val calibanRouteZIO: ZIO[ModelObjectDataService, CalibanError.ValidationError, HttpApp[ModelObjectDataService, Throwable]] = for {
//  interpreter <- modelObjectApi.interpreter
//} yield caliban.ZHttpAdapter.makeHttpService(interpreter)
