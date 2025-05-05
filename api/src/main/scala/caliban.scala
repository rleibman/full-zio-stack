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

//import caliban.GraphQL.graphQL
//import caliban.schema.Schema
//import caliban.*
//import db.*
//import model.*
//import zio.http.*
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
