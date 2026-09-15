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

package net.leibman.fullziostack.graphql

import caliban.CalibanError.ExecutionError
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import net.leibman.fullziostack.db.{DataServiceException, ModelObjectDataService}
import net.leibman.fullziostack.model.*
import zio.*

/** The business logic behind the GraphQL API. Failures are Caliban errors whose `extensions.code` tells clients what went wrong (see
  * [[FullZIOStackService.ErrorCode]]).
  */
trait FullZIOStackService {

  def modelObjects(search: ModelObjectSearch): IO[ExecutionError, Page[ModelObject]]

  def modelObject(id: ModelObjectId): IO[ExecutionError, Option[ModelObject]]

  def upsertModelObject(modelObject: ModelObject): IO[ExecutionError, ModelObject]

  def deleteModelObject(
    id:         ModelObjectId,
    softDelete: Boolean
  ): IO[ExecutionError, Boolean]

}

object FullZIOStackService {

  enum ErrorCode {

    case BAD_USER_INPUT, NOT_FOUND, CONFLICT, UNAVAILABLE, INTERNAL

  }

  def error(
    code:    ErrorCode,
    message: String,
    cause:   Option[Throwable] = None
  ): ExecutionError =
    ExecutionError(
      message,
      innerThrowable = cause,
      extensions = Some(ObjectValue(List("code" -> StringValue(code.toString))))
    )

  /** Expected failures become errors the client can act on. Unexpected ones are logged here and reported without details, so no internals leak to
    * clients.
    */
  def fromDataServiceException(e: DataServiceException): UIO[ExecutionError] =
    e match {
      case DataServiceException.NotFound(entity, id) => ZIO.succeed(error(ErrorCode.NOT_FOUND, s"$entity $id not found"))
      case e: DataServiceException.Conflict  => ZIO.succeed(error(ErrorCode.CONFLICT, e.message))
      case e: DataServiceException.Transient =>
        ZIO.logWarningCause("Transient data error", Cause.fail(e)).as(error(ErrorCode.UNAVAILABLE, "Temporarily unavailable, try again", Some(e)))
      case e: DataServiceException.Unexpected =>
        ZIO.logErrorCause("Unexpected data error", Cause.fail(e)).as(error(ErrorCode.INTERNAL, "Internal error", Some(e)))
    }

  val live: URLayer[ModelObjectDataService, FullZIOStackService] = ZLayer.fromFunction(Live.apply)

  final private case class Live(modelObjectDataService: ModelObjectDataService) extends FullZIOStackService {

    private def run[A](io: IO[DataServiceException, A]): IO[ExecutionError, A] =
      io.flatMapError(fromDataServiceException)

    override def modelObjects(search: ModelObjectSearch): IO[ExecutionError, Page[ModelObject]] =
      if (search.limit < 1 || search.limit > 500 || search.offset < 0)
        ZIO.fail(error(ErrorCode.BAD_USER_INPUT, "offset must be >= 0 and limit between 1 and 500"))
      else run(modelObjectDataService.search(search))

    override def modelObject(id: ModelObjectId): IO[ExecutionError, Option[ModelObject]] = run(modelObjectDataService.get(id))

    override def upsertModelObject(modelObject: ModelObject): IO[ExecutionError, ModelObject] =
      if (modelObject.name.isBlank) ZIO.fail(error(ErrorCode.BAD_USER_INPUT, "name must not be blank"))
      else run(modelObjectDataService.upsert(modelObject.copy(name = modelObject.name.trim.nn)))

    override def deleteModelObject(
      id:         ModelObjectId,
      softDelete: Boolean
    ): IO[ExecutionError, Boolean] = run(modelObjectDataService.delete(id, softDelete))

  }

}
