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

package net.leibman.fullziostack.db

import doobie.*
import doobie.implicits.*
import net.leibman.fullziostack.db.DoobieMappings.given
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.repository.Repository.ModelObjectOperations
import net.leibman.fullziostack.repository.RepositoryError
import zio.*
import zio.interop.catz.*

import java.time.temporal.ChronoUnit

final case class DoobieModelObjectOperations(transactor: Transactor[Task]) extends ModelObjectOperations[DataIO] {

  private given Meta[ModelObjectId] = Meta[Int].timap(ModelObjectId.apply)(_.value)
  private given Meta[ModelObjectType] = Meta[String].timap(ModelObjectType.valueOf)(_.toString)

  private val now = Clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))

  private def execute[A](program: ConnectionIO[A]): DataIO[A] = program.transact(transactor).mapError(RepositoryErrors.fromThrowable)

  private val columns = fr"id, name, description, model_object_type, deleted, created, last_updated"

  private def matching(search: ModelObjectSearch): Fragment =
    Fragments.whereAndOpt(
      Option.unless(search.includeDeleted)(fr"deleted = ${false}"),
      search.text.map { text =>
        val pattern = s"%${text.toLowerCase}%"
        fr"(lower(name) like $pattern or lower(description) like $pattern)"
      },
      search.modelObjectType.map(modelObjectType => fr"model_object_type = $modelObjectType")
    )

  override def search(search: ModelObjectSearch): DataIO[Page[ModelObject]] =
    execute(for {
      items <- (fr"select" ++ columns ++ fr"from model_object" ++ matching(search) ++
        fr"order by id limit ${search.limit} offset ${search.offset}").query[ModelObject].to[Seq]
      total <- (fr"select count(*) from model_object" ++ matching(search)).query[Long].unique
    } yield Page(items, total))

  override def get(id: ModelObjectId): DataIO[Option[ModelObject]] =
    execute((fr"select" ++ columns ++ fr"from model_object where id = $id").query[ModelObject].option)

  override def upsert(entity: ModelObject): DataIO[ModelObject] =
    now.flatMap { timestamp =>
      if (entity.id == ModelObjectId.empty) {
        val row = entity.copy(created = timestamp, lastUpdated = timestamp)
        execute(
          sql"""insert into model_object (name, description, model_object_type, deleted, created, last_updated)
                values (${row.name}, ${row.description}, ${row.modelObjectType}, ${row.deleted}, ${row.created}, ${row.lastUpdated})""".update
            .withUniqueGeneratedKeys[ModelObjectId]("id")
        ).map(id => row.copy(id = id))
      } else {
        for {
          updated <- execute(
            sql"""update model_object
                  set name = ${entity.name}, description = ${entity.description}, model_object_type = ${entity.modelObjectType},
                      deleted = ${entity.deleted}, last_updated = $timestamp
                  where id = ${entity.id}""".update.run
          )
          _     <- ZIO.fail(RepositoryError.NotFound(s"ModelObject ${entity.id.value} not found")).when(updated == 0)
          saved <- get(entity.id).someOrFail(RepositoryError.NotFound(s"ModelObject ${entity.id.value} not found"))
        } yield saved
      }
    }

  override def delete(
    id:         ModelObjectId,
    softDelete: Boolean
  ): DataIO[Boolean] =
    if (softDelete)
      now
        .flatMap(timestamp => execute(sql"update model_object set deleted = ${true}, last_updated = $timestamp where id = $id".update.run).map(_ > 0))
    else execute(sql"delete from model_object where id = $id".update.run).map(_ > 0)

}
