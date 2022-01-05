/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import io.getquill.*
import io.getquill.context.ZioJdbc.*
import model.*
import zio.*
import zio.console.putStrLn

import java.sql.SQLException
import java.time.LocalDateTime
import javax.sql.DataSource

package object db {
  given MappedEncoding [ModelObjectId, Int] (_.asInt)
  given MappedEncoding [Int, ModelObjectId] (ModelObjectId.apply)
  given MappedEncoding [ModelObjectType, String] (_.toString)
  given MappedEncoding [String, ModelObjectType] (ModelObjectType.valueOf)

  object QuillContext extends MysqlZioJdbcContext(SnakeCase) {
    val dataSourceLayer: ULayer[Has[DataSource]] =
      DataSourceLayer.fromPrefix("database").orDie
  }

  trait DataService {
    def all: IO[SQLException, List[ModelObject]]
    def get(modelObjectId:    ModelObjectId): IO[SQLException, Option[ModelObject]]
    def delete(modelObjectId: ModelObjectId): IO[SQLException, Boolean]
    def upsert(modelObject:   ModelObject):   IO[SQLException, ModelObject]

    def delete(modelObject: ModelObject): IO[SQLException, Boolean] = delete(modelObject.id)
  }

  object DataService {
    val live: URLayer[Has[DataSource], Has[DataService]] = (DataServiceLive.apply _).toLayer[DataService]
  }

  import QuillContext.*

  final case class DataServiceLive(dataSource: DataSource) extends DataService {
    val env: Has[DataSource] = Has(dataSource)

    override def all: IO[SQLException, List[ModelObject]] = run(query[ModelObject]).provide(env)
    override def get(id: ModelObjectId): IO[SQLException, Option[ModelObject]] =
      run(quote(query[ModelObject].filter(_.id == lift(id)))).provide(env).map(_.headOption)
    override def delete(id: ModelObjectId): IO[SQLException, Boolean] = {
      run(quote(query[ModelObject].filter(_.id == lift(id)).delete)).provide(env).map(_ > 0)
    }
    override def upsert(modelObject: ModelObject): IO[SQLException, ModelObject] = {
      if (modelObject.id == ModelObjectId.empty) {
        run(quote {
          query[ModelObject]
            .insert(
              _.name            -> modelObject.name,
              _.description     -> modelObject.description,
              _.deleted         -> false,
              _.created         -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
              _.lastUpdated     -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
              _.modelObjectType -> lift(modelObject.modelObjectType)
            )
            .returningGenerated(_.id)
        }).provide(env).map(id => modelObject.copy(id = id))
      } else {
        run(quote {
          query[ModelObject]
            .filter(_.id == lift(modelObject.id))
            .update(
              _.name            -> modelObject.name,
              _.description     -> modelObject.description,
              _.deleted         -> false,
              _.lastUpdated     -> lift(LocalDateTime.now.asInstanceOf[LocalDateTime]),
              _.modelObjectType -> lift(modelObject.modelObjectType)
            )
        }).provide(env).map(_ => modelObject) //TODO need to return the newly inserted object id.
      }
    }
  }

  def main(args: Array[String]): Unit = {
    println(io.getquill.util.Messages.qprint(quote(query[ModelObject]).ast))
  }
}
