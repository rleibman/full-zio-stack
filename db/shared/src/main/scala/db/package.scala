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

package db

import model.*
import zio.*

type DBIO[A] = IO[DataServiceException, A]

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

trait DataService[F[_], PK, TYPE, SEARCH] {

  def search(search: Option[SEARCH] = None): F[IndexedSeq[TYPE]]
  def get(id:        PK):                    F[Option[TYPE]]
  def delete(
    id:         PK,
    softDelete: Boolean
  ):                        F[Boolean]
  def upsert(obj:    TYPE): F[TYPE]
  def extractPK(obj: TYPE): PK

}

trait ModelObjectDataService[F[_]] extends DataService[F, ModelObjectId, ModelObject, Nothing] {

  def extractPK(obj: ModelObject): ModelObjectId = obj.id

}

trait DataServices[F[_]] {

  def modelObjectDS: DataService[F, ModelObjectId, ModelObject, Nothing]

}
