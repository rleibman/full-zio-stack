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

import caliban.*
import caliban.CalibanError.ExecutionError
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import caliban.schema.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.wrappers.Wrappers.*
import net.leibman.fullziostack.db.{DataIO, ZIORepository}
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.repository.RepositoryError
import net.leibman.fullziostack.server.BuildInfo
import zio.*

import scala.language.postfixOps

/** The GraphQL API, resolved directly against the [[ZIORepository]]. After changing it, run `calibanRender` to update the committed schema.graphql,
  * which the client's generated code is built from (SchemaSpec fails until you do).
  *
  * Failures are Caliban errors whose `extensions.code` is an [[ErrorCode]], so clients can tell them apart.
  */
object FullZIOStackApi {

  type Op[A] = ZIO[ZIORepository, ExecutionError, A]

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
  private def toExecutionError(e: RepositoryError): UIO[ExecutionError] =
    e match {
      case RepositoryError.NotFound(message)    => ZIO.succeed(error(ErrorCode.NOT_FOUND, message))
      case RepositoryError.Invalid(message)     => ZIO.succeed(error(ErrorCode.BAD_USER_INPUT, message))
      case RepositoryError.Conflict(message, _) => ZIO.succeed(error(ErrorCode.CONFLICT, message))
      case e: RepositoryError.Transient =>
        ZIO
          .logWarningCause("Transient repository error", Cause.fail(e)).as(
            error(ErrorCode.UNAVAILABLE, "Temporarily unavailable, try again", Some(e))
          )
      case e: RepositoryError.Unexpected =>
        ZIO.logErrorCause("Unexpected repository error", Cause.fail(e)).as(error(ErrorCode.INTERNAL, "Internal error", Some(e)))
    }

  /** Runs a repository operation, turning its failures into API errors. */
  private def withRepository[A](operation: ZIORepository => DataIO[A]): Op[A] =
    ZIO.serviceWithZIO[ZIORepository](operation).flatMapError(toExecutionError)

  private def invalid(message: String): Op[Nothing] = ZIO.fail(error(ErrorCode.BAD_USER_INPUT, message))

  /** Derives schemas for types whose fields need the repository (Schema.auto only covers Any). */
  object ApiSchema extends GenericSchema[ZIORepository]
  import ApiSchema.auto.*

  case class ModelObjectArgs(id: ModelObjectId)

  case class ModelObjectsArgs(search: ModelObjectSearch)

  case class UpsertModelObjectArgs(modelObject: ModelObject)

  case class DeleteModelObjectArgs(
    id:         ModelObjectId,
    softDelete: Boolean
  )

  case class Queries(
    @GQLDescription("Model objects matching the search, one page at a time")
    modelObjects: ModelObjectsArgs => Op[Page[ModelObject]],
    @GQLDescription("A model object by id, including soft-deleted ones")
    modelObject: ModelObjectArgs => Op[Option[ModelObject]],
    @GQLDescription("The server's version")
    version: String
  )

  case class Mutations(
    @GQLDescription("Creates the model object when its id is -1, otherwise updates it. Returns what was saved.")
    upsertModelObject: UpsertModelObjectArgs => Op[ModelObject],
    @GQLDescription("Soft delete marks the object as deleted; hard delete removes it. False if there was nothing to delete.")
    deleteModelObject: DeleteModelObjectArgs => Op[Boolean]
  )

  given Schema[Any, ModelObjectId] = Schema.intSchema.contramap(_.value)
  given ArgBuilder[ModelObjectId] = ArgBuilder.int.map(ModelObjectId.apply)
  // Explicit, so graphQL(...) below is typed for ZIORepository instead of inferring Any.
  given Schema[ZIORepository, Queries] = ApiSchema.gen[ZIORepository, Queries]
  given Schema[ZIORepository, Mutations] = ApiSchema.gen[ZIORepository, Mutations]

  lazy val api: GraphQL[ZIORepository] =
    graphQL(
      RootResolver(
        Queries(
          modelObjects = args =>
            if (args.search.limit < 1 || args.search.limit > 500 || args.search.offset < 0)
              invalid("offset must be >= 0 and limit between 1 and 500")
            else withRepository(_.modelObjectOps.search(args.search)),
          modelObject = args => withRepository(_.modelObjectOps.get(args.id)),
          version = BuildInfo.version
        ),
        Mutations(
          upsertModelObject = args =>
            if (args.modelObject.name.isBlank) invalid("name must not be blank")
            else withRepository(_.modelObjectOps.upsert(args.modelObject.copy(name = args.modelObject.name.trim.nn))),
          deleteModelObject = args => withRepository(_.modelObjectOps.delete(args.id, args.softDelete))
        )
      )
    ) @@ maxFields(200) // query analyzer that limit query fields
      @@ maxDepth(30) // query analyzer that limit query depth
      @@ timeout(3 seconds) // wrapper that fails slow queries
      @@ printSlowQueries(500 millis) // wrapper that logs slow queries

}
