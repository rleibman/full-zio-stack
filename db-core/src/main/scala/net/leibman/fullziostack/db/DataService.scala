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

import net.leibman.fullziostack.model.Page
import zio.*

import java.sql.{SQLException, SQLRecoverableException, SQLTransientException}

/** What every data service returns: it either succeeds or fails with a [[DataServiceException]]. It never throws. */
type DataIO[A] = IO[DataServiceException, A]

sealed abstract class DataServiceException(
  message: String,
  cause:   Option[Throwable]
) extends Exception(message, cause.orNull) {

  /** True when retrying later might succeed (lost connection, timeout...). */
  def isTransient: Boolean = false

}

object DataServiceException {

  final case class NotFound(
    entity: String,
    id:     String
  ) extends DataServiceException(s"$entity $id not found", None)

  /** The operation broke a constraint: duplicate key, foreign key... */
  final case class Conflict(
    message: String,
    cause:   Option[Throwable] = None
  ) extends DataServiceException(message, cause)

  final case class Transient(
    message: String,
    cause:   Option[Throwable] = None
  ) extends DataServiceException(message, cause) {

    override def isTransient: Boolean = true

  }

  final case class Unexpected(
    message: String,
    cause:   Option[Throwable] = None
  ) extends DataServiceException(message, cause)

  /** Classifies a failure from a database library. SQL states: class 08 is connection problems, class 23 is integrity constraint violations.
    */
  def fromThrowable(t: Throwable): DataServiceException = {
    val message: String = Option(t.getMessage).getOrElse(t.getClass.getName)
    t match {
      case e: DataServiceException                            => e
      case _: SQLTransientException                           => Transient(message, Some(t))
      case _: SQLRecoverableException                         => Transient(message, Some(t))
      case e: SQLException if sqlStateClass(e).contains("08") => Transient(message, Some(t))
      case e: SQLException if sqlStateClass(e).contains("23") => Conflict(message, Some(t))
      case _ => Unexpected(message, Some(t))
    }
  }

  private def sqlStateClass(e: SQLException): Option[String] = Option(e.getSQLState).map(_.take(2))

}

/** CRUD for one entity type.
  *
  * @tparam PK
  *   the entity's id type
  * @tparam T
  *   the entity
  * @tparam S
  *   the search criteria
  */
trait DataService[PK, T, S] {

  def search(search: S): DataIO[Page[T]]

  def get(id: PK): DataIO[Option[T]]

  /** Inserts `obj` when its id is the entity's `empty` id, otherwise updates it. Returns the saved entity, with the id and timestamps set by the
    * database layer. Updating an id that doesn't exist fails with `NotFound`.
    */
  def upsert(obj: T): DataIO[T]

  /** Soft delete marks the entity as deleted; hard delete removes it. Returns false if there was nothing to delete. */
  def delete(
    id:         PK,
    softDelete: Boolean
  ): DataIO[Boolean]

}
