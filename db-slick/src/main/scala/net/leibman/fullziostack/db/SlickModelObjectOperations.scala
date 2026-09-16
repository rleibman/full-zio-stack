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

import net.leibman.fullziostack.db.SlickProfile.instantColumnType
import net.leibman.fullziostack.db.SlickProfile.profile.api.*
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.repository.Repository.ModelObjectOperations
import net.leibman.fullziostack.repository.RepositoryError
// Selectively: zio.* would clash with Slick's Tag.
import zio.{Clock, ZIO}

import java.time.Instant
import java.time.temporal.ChronoUnit

final case class SlickModelObjectOperations(database: Database) extends ModelObjectOperations[DataIO] {

  private given BaseColumnType[ModelObjectId] = MappedColumnType.base[ModelObjectId, Int](_.value, ModelObjectId.apply)
  private given BaseColumnType[ModelObjectType] = MappedColumnType.base[ModelObjectType, String](_.toString, ModelObjectType.valueOf)

  private class ModelObjects(tag: Tag) extends Table[ModelObject](tag, "model_object") {

    def id = column[ModelObjectId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")
    def modelObjectType = column[ModelObjectType]("model_object_type")
    def deleted = column[Boolean]("deleted")
    def created = column[Instant]("created")
    def lastUpdated = column[Instant]("last_updated")
    def * = (id, name, description, modelObjectType, deleted, created, lastUpdated).mapTo[ModelObject]

  }

  private val modelObjects = TableQuery[ModelObjects]

  private val now = Clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))

  private def execute[A](action: DBIO[A]): DataIO[A] =
    ZIO.fromFuture(_ => database.run(action)).mapError(RepositoryErrors.fromThrowable)

  private def byId(id: ModelObjectId) = modelObjects.filter(_.id === id)

  override def search(search: ModelObjectSearch): DataIO[Page[ModelObject]] = {
    val pattern = search.text.map(text => s"%${text.toLowerCase}%")
    val matching = modelObjects
      .filterIf(!search.includeDeleted)(o => !o.deleted)
      .filterOpt(pattern)(
        (
          o,
          p
        ) => o.name.toLowerCase.like(p) || o.description.toLowerCase.like(p)
      )
      .filterOpt(search.modelObjectType)(
        (
          o,
          modelObjectType
        ) => o.modelObjectType === modelObjectType
      )
    // zip rather than flatMap: DBIO's map/flatMap need an ExecutionContext, and ZIO does the mapping instead.
    execute(matching.sortBy(_.id).drop(search.offset).take(search.limit).result.zip(matching.length.result))
      .map(
        (
          items,
          total
        ) => Page(items, total.toLong)
      )
  }

  override def get(id: ModelObjectId): DataIO[Option[ModelObject]] = execute(byId(id).result.headOption)

  override def upsert(entity: ModelObject): DataIO[ModelObject] =
    now.flatMap { timestamp =>
      if (entity.id == ModelObjectId.empty) {
        val row = entity.copy(created = timestamp, lastUpdated = timestamp)
        execute((modelObjects returning modelObjects.map(_.id)) += row).map(id => row.copy(id = id))
      } else {
        for {
          updated <- execute(
            byId(entity.id)
              .map(o => (o.name, o.description, o.modelObjectType, o.deleted, o.lastUpdated))
              .update((entity.name, entity.description, entity.modelObjectType, entity.deleted, timestamp))
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
      now.flatMap(timestamp => execute(byId(id).map(o => (o.deleted, o.lastUpdated)).update((true, timestamp)))).map(_ > 0)
    else execute(byId(id).delete).map(_ > 0)

}
