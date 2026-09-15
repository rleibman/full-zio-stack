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

import io.getquill.*
import net.leibman.fullziostack.db.QuillContext.*
import net.leibman.fullziostack.model.*
import zio.*

import java.sql.SQLException
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

final case class QuillModelObjectDataService(dataSource: DataSource) extends ModelObjectDataService {

  private given MappedEncoding[ModelObjectId, Int] = MappedEncoding(_.value)
  private given MappedEncoding[Int, ModelObjectId] = MappedEncoding(ModelObjectId.apply)
  private given MappedEncoding[ModelObjectType, String] = MappedEncoding(_.toString)
  private given MappedEncoding[String, ModelObjectType] = MappedEncoding(ModelObjectType.valueOf)

  private val now = Clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))

  private def execute[A](query: ZIO[DataSource, SQLException, A]): DataIO[A] =
    query.provideEnvironment(ZEnvironment(dataSource)).mapError(DataServiceException.fromThrowable)

  // Inline parameters keep the query static (compiled to SQL at build time): Quill can't lift a value defined inside
  // the quotation's own scope.
  inline private def matching(
    inline pattern:         Option[String],
    inline includeDeleted:  Boolean,
    inline modelObjectType: Option[ModelObjectType]
  ) =
    quote {
      query[ModelObject].filter(o =>
        (lift(includeDeleted) || !o.deleted) &&
          lift(pattern).forall(p => o.name.toLowerCase.like(p) || o.description.toLowerCase.like(p)) &&
          lift(modelObjectType).forall(t => o.modelObjectType == t)
      )
    }

  override def search(search: ModelObjectSearch): DataIO[Page[ModelObject]] = {
    val pattern = search.text.map(text => s"%${text.toLowerCase}%")
    val includeDeleted = search.includeDeleted
    val modelObjectType = search.modelObjectType
    val offset = search.offset
    val limit = search.limit
    for {
      items <- execute(
        run(matching(pattern, includeDeleted, modelObjectType).sortBy(_.id).drop(lift(offset)).take(lift(limit)))
      )
      total <- execute(run(matching(pattern, includeDeleted, modelObjectType).size))
    } yield Page(items, total)
  }

  override def get(id: ModelObjectId): DataIO[Option[ModelObject]] =
    execute(run(query[ModelObject].filter(_.id == lift(id)))).map(_.headOption)

  override def upsert(obj: ModelObject): DataIO[ModelObject] =
    now.flatMap { timestamp =>
      if (obj.id == ModelObjectId.empty) {
        val row = obj.copy(created = timestamp, lastUpdated = timestamp)
        execute(run(query[ModelObject].insertValue(lift(row)).returningGenerated(_.id))).map(id => row.copy(id = id))
      } else {
        for {
          updated <- execute(
            run(
              query[ModelObject]
                .filter(_.id == lift(obj.id))
                .update(
                  _.name            -> lift(obj.name),
                  _.description     -> lift(obj.description),
                  _.modelObjectType -> lift(obj.modelObjectType),
                  _.deleted         -> lift(obj.deleted),
                  _.lastUpdated     -> lift(timestamp)
                )
            )
          )
          _     <- ZIO.fail(DataServiceException.NotFound("ModelObject", obj.id.value.toString)).when(updated == 0L)
          saved <- get(obj.id).someOrFail(DataServiceException.NotFound("ModelObject", obj.id.value.toString))
        } yield saved
      }
    }

  override def delete(
    id:         ModelObjectId,
    softDelete: Boolean
  ): DataIO[Boolean] =
    if (softDelete) {
      now
        .flatMap(timestamp =>
          execute(
            run(query[ModelObject].filter(_.id == lift(id)).update(_.deleted -> lift(true), _.lastUpdated -> lift(timestamp)))
          )
        ).map(_ > 0L)
    } else {
      // Not query[ModelObject].filter(...).delete: Quill aliases the table (`DELETE FROM model_object x WHERE ...`),
      // which MariaDB only accepts from 11.6 on.
      execute(run(sql"DELETE FROM model_object WHERE id = ${lift(id)}".as[Delete[ModelObject]])).map(_ > 0L)
    }

}
