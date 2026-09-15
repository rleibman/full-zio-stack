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

import net.leibman.fullziostack.repository.{Repository, RepositoryError}
import zio.*

import java.sql.{SQLException, SQLRecoverableException, SQLTransientException}

/** What every server-side repository operation returns: it either succeeds or fails with a [[RepositoryError]]. It never throws.
  */
type DataIO[A] = IO[RepositoryError, A]

/** The repository, implemented with ZIO over the database. Each DB layer provides `ZIORepository.live`. */
type ZIORepository = Repository[DataIO]

object RepositoryErrors {

  /** Classifies a failure from a database library. SQL states: class 08 is connection problems, class 23 is integrity constraint violations.
    */
  def fromThrowable(t: Throwable): RepositoryError = {
    val message: String = Option(t.getMessage).getOrElse(t.getClass.getName)
    t match {
      case e: RepositoryError                                 => e
      case _: SQLTransientException                           => RepositoryError.Transient(message, Some(t))
      case _: SQLRecoverableException                         => RepositoryError.Transient(message, Some(t))
      case e: SQLException if sqlStateClass(e).contains("08") => RepositoryError.Transient(message, Some(t))
      case e: SQLException if sqlStateClass(e).contains("23") => RepositoryError.Conflict(message, Some(t))
      case _ => RepositoryError.Unexpected(message, Some(t))
    }
  }

  private def sqlStateClass(e: SQLException): Option[String] = Option(e.getSQLState).map(_.take(2))

}
