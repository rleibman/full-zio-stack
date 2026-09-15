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

import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.repository.Repository.ModelObjectOperations
import net.leibman.fullziostack.repository.RepositoryError
import zio.*

import java.time.temporal.ChronoUnit

/** In-memory ModelObject operations with the same behaviour as the real ones (they pass the same contract suite). */
final case class MockModelObjectOperations(
  rows:   Ref[Map[ModelObjectId, ModelObject]],
  nextId: Ref[Int]
) extends ModelObjectOperations[DataIO] {

  private val now = Clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))

  override def search(search: ModelObjectSearch): DataIO[Page[ModelObject]] =
    rows.get.map { all =>
      val text = search.text.map(_.toLowerCase)
      val matching = all.values.toSeq
        .filter(o => search.includeDeleted || !o.deleted)
        .filter(o => text.forall(t => o.name.toLowerCase.contains(t) || o.description.toLowerCase.contains(t)))
        .filter(o => search.modelObjectType.forall(_ == o.modelObjectType))
        .sortBy(_.id.value)
      Page(matching.slice(search.offset, search.offset + search.limit), matching.size.toLong)
    }

  override def get(id: ModelObjectId): DataIO[Option[ModelObject]] = rows.get.map(_.get(id))

  override def upsert(entity: ModelObject): DataIO[ModelObject] =
    now.flatMap { timestamp =>
      if (entity.id == ModelObjectId.empty) {
        for {
          id <- nextId.getAndUpdate(_ + 1)
          saved = entity.copy(id = ModelObjectId(id), created = timestamp, lastUpdated = timestamp)
          _ <- rows.update(_ + (saved.id -> saved))
        } yield saved
      } else {
        rows
          .modify { all =>
            all.get(entity.id) match {
              case None           => (None, all)
              case Some(existing) =>
                val saved = entity.copy(created = existing.created, lastUpdated = timestamp)
                (Some(saved), all + (saved.id -> saved))
            }
          }.someOrFail(RepositoryError.NotFound(s"ModelObject ${entity.id.value} not found"))
      }
    }

  override def delete(
    id:         ModelObjectId,
    softDelete: Boolean
  ): DataIO[Boolean] =
    now.flatMap { timestamp =>
      rows.modify { all =>
        all.get(id) match {
          case None                         => (false, all)
          case Some(existing) if softDelete => (true, all + (id -> existing.copy(deleted = true, lastUpdated = timestamp)))
          case Some(_)                      => (true, all - id)
        }
      }
    }

}
