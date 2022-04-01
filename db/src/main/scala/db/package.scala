/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import io.getquill.*
import io.getquill.context.ZioJdbc.*
import model.*
import zio.*
import zio.Console.printLine

import java.time.LocalDateTime
import javax.sql.DataSource

//TODO Figure out streaming
//
package object db {

  class DataServiceException(
    val message: String = "",
    val cause:   Option[Throwable]
  ) extends Exception(message, cause.orNull) {}
//  object QuillContext extends MysqlZioJdbcContext(SnakeCase) {
//    val dataSourceLayer: ULayer[Has[DataSource]] =
//      DataSourceLayer.fromPrefix("database").orDie
//  }
  object DataService {}

  trait DataService[PK, TYPE, SEARCH] {

    def search(search: Option[SEARCH] = None): IO[DataServiceException, List[TYPE]]
    def get(id:        PK):                    IO[DataServiceException, Option[TYPE]]
    def delete(
      id:         PK,
      softDelete: Boolean
    ): IO[DataServiceException, Boolean]
    def upsert(obj:    TYPE): IO[DataServiceException, TYPE]
    def extractPK(obj: TYPE): PK

  }

  trait ModelObjectDataService extends DataService[ModelObjectId, ModelObject, Nothing] {

    def extractPK(obj: ModelObject): ModelObjectId = obj.id

  }

  trait DataServices {

    def modelObjectDS: DataService[ModelObjectId, ModelObject, Nothing]

  }

//  object DataService {
//    val modelObjectDataServiceLive: URLayer[Has[DataSource], Has[DataService[ModelObjectId, ModelObject]]] =
//      (ModelObjectDataService.apply _).toLayer[DataService[ModelObjectId, ModelObject]]
//  }
//
//  import QuillContext.*
//
//  final case class ModelObjectDataService(dataSource: DataSource) extends DataService[ModelObjectId, ModelObject] {
//    val env: DataSource = dataSource
//    given MappedEncoding [ModelObjectId, Int] (_.asInt)
//    given MappedEncoding [Int, ModelObjectId] (ModelObjectId.apply)
//    given MappedEncoding [ModelObjectType, String] (_.toString)
//    given MappedEncoding [String, ModelObjectType] (ModelObjectType.valueOf)
//
//    override def all: IO[SQLException, List[ModelObject]] = run(query[ModelObject]).provide(env)
//    override def get(id: ModelObjectId): IO[SQLException, Option[ModelObject]] =
//      run(quote(query[ModelObject].filter(_.id == lift(id)))).provide(env).map(_.headOption)
//    override def delete(
//      id:         ModelObjectId,
//      softDelete: Boolean
//    ): IO[SQLException, Boolean] = {
//      if (softDelete) {
//        run(quote {
//          query[ModelObject]
//            .filter(_.id == lift(id))
//            .update(
//              _.deleted     -> true,
//              _.lastUpdated -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime])
//            )
//        }).provide(env).map(_ > 0) //TODO need to return the newly inserted object id.
//      } else {
//        run(quote(query[ModelObject].filter(_.id == lift(id)).delete)).provide(env).map(_ > 0)
//      }
//    }
//    override def upsert(modelObject: ModelObject): IO[SQLException, ModelObject] = {
//      if (modelObject.id == ModelObjectId.empty) {
//        run(quote {
//          query[ModelObject]
//            .insert(
//              _.name            -> lift(modelObject.name),
//              _.description     -> lift(modelObject.description),
//              _.deleted         -> false,
//              _.created         -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
//              _.lastUpdated     -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
//              _.modelObjectType -> lift(modelObject.modelObjectType)
//            )
//            .returningGenerated(_.id)
//        }).provide(env).map(id => modelObject.copy(id = id))
//      } else {
//        run(quote {
//          query[ModelObject]
//            .filter(_.id == lift(modelObject.id))
//            .update(
//              _.name            -> lift(modelObject.name),
//              _.description     -> lift(modelObject.description),
//              _.deleted         -> false,
//              _.lastUpdated     -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
//              _.modelObjectType -> lift(modelObject.modelObjectType)
//            )
//        }).provide(env).map(_ => modelObject) //TODO need to return the newly inserted object id.
//      }
//    }
//  }
//
//  def main(args: Array[String]): Unit = {
//    println(io.getquill.util.Messages.qprint(quote(query[ModelObject]).ast))
//  }
}
