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

import model.*
import zio.*
import zio.Console.printLine

import java.time.LocalDateTime
import javax.sql.DataSource

//TODO Figure out streaming
//
package object db {

  import scala.language.unsafeNulls

  object DataServiceException {

    def apply(t: Throwable) =
      t match {
        case t: DataServiceException => t
        case cause => new DataServiceException("", Some(cause))
      }

  }
  class DataServiceException(
    val message:     String = "",
    val cause:       Option[Throwable],
    val isTransient: Boolean = false
  ) extends Exception(message, cause.orNull)
//  object QuillContext extends MysqlZioJdbcContext(SnakeCase) {
//    val dataSourceLayer: ULayer[Has[DataSource]] =
//      DataSourceLayer.fromPrefix("database").orDie
//  }
  object DataService {}

  trait DataService[PK, TYPE, SEARCH] {

    def search(search: Option[SEARCH] = None): IO[DataServiceException, IndexedSeq[TYPE]]
    def get(id:        PK):                    IO[DataServiceException, Option[TYPE]]
    def delete(
      id:         PK,
      softDelete: Boolean
    ):                        IO[DataServiceException, Boolean]
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
