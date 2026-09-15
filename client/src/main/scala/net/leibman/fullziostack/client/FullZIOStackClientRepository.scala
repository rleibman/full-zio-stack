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

package net.leibman.fullziostack.client

import caliban.client.CalibanClientError.ServerError
import caliban.client.Operations.IsOperation
import caliban.client.SelectionBuilder
import caliban.client.__Value.{__ObjectValue, __StringValue}
import japgolly.scalajs.react.AsyncCallback
import net.leibman.fullziostack.client.api.FullZIOStackClient.{
  ModelObject as GqlModelObject,
  ModelObjectInput,
  ModelObjectSearchInput,
  ModelObjectType as GqlModelObjectType,
  Mutations,
  PageModelObject,
  Queries
}
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.repository.Repository.ModelObjectOperations
import net.leibman.fullziostack.repository.{Repository, RepositoryError}
import org.scalajs.dom
import sttp.client4.*
import sttp.client4.fetch.FetchBackend

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

/** The repository, implemented over the server's GraphQL API. Failures are the same [[RepositoryError]]s the server raised, rebuilt from the errors'
  * `extensions.code`.
  */
object FullZIOStackClientRepository extends Repository[AsyncCallback] {

  private val backend = FetchBackend()

  // Same origin as the page.
  lazy private val endpoint = uri"${dom.window.location.origin}/api/graphql"

  /** Runs an operation built with the generated FullZIOStackClient. */
  private def run[Origin: IsOperation, A](selection: SelectionBuilder[Origin, A]): AsyncCallback[A] =
    AsyncCallback
      .fromFuture(selection.toRequest(endpoint).send(backend))
      .flatMap(response =>
        response.body match {
          case Right(result)             => AsyncCallback.pure(result)
          case Left(ServerError(errors)) =>
            val message = errors.map(_.message).mkString("; ")
            val code = errors
              .flatMap(_.extensions).collectFirst { case __ObjectValue(fields) =>
                fields.collectFirst { case ("code", __StringValue(code)) => code }
              }.flatten
            AsyncCallback.throwException(code match {
              case Some("NOT_FOUND")      => RepositoryError.NotFound(message)
              case Some("BAD_USER_INPUT") => RepositoryError.Invalid(message)
              case Some("CONFLICT")       => RepositoryError.Conflict(message)
              case Some("UNAVAILABLE")    => RepositoryError.Transient(message)
              case _                      => RepositoryError.Unexpected(message)
            })
          case Left(error) => AsyncCallback.throwException(RepositoryError.Transient(s"Couldn't reach the server (${error.getMessage})", Some(error)))
        }
      )

  // ── ModelObject ────────────────────────────────────────────────────────────

  private def fromGql(modelObjectType: GqlModelObjectType): ModelObjectType = ModelObjectType.valueOf(modelObjectType.value)

  private def toGql(modelObjectType: ModelObjectType): GqlModelObjectType =
    GqlModelObjectType.values.find(_.value == modelObjectType.toString).getOrElse(GqlModelObjectType.type1)

  private val modelObjectSelection: SelectionBuilder[GqlModelObject, ModelObject] =
    (GqlModelObject.id ~ GqlModelObject.name ~ GqlModelObject.description ~ GqlModelObject.modelObjectType ~
      GqlModelObject.deleted ~ GqlModelObject.created ~ GqlModelObject.lastUpdated).mapN {
      // mapN is overloaded by arity, so the parameter types can't be inferred.
      (
        id:              Int,
        name:            String,
        description:     String,
        modelObjectType: GqlModelObjectType,
        deleted:         Boolean,
        created:         String,
        lastUpdated:     String
      ) =>
        ModelObject(
          id = ModelObjectId(id),
          name = name,
          description = description,
          modelObjectType = fromGql(modelObjectType),
          deleted = deleted,
          created = Instant.parse(created),
          lastUpdated = Instant.parse(lastUpdated)
        )
    }

  override val modelObjectOps: ModelObjectOperations[AsyncCallback] = new ModelObjectOperations[AsyncCallback] {

    override def search(search: ModelObjectSearch): AsyncCallback[Page[ModelObject]] =
      run(
        Queries.modelObjects(
          ModelObjectSearchInput(
            text = search.text,
            modelObjectType = search.modelObjectType.map(toGql),
            includeDeleted = search.includeDeleted,
            offset = search.offset,
            limit = search.limit
          )
        )(PageModelObject.items(modelObjectSelection) ~ PageModelObject.total)
      ).map(
        _.fold(Page(Seq.empty[ModelObject], 0L))(
          (
            items,
            total
          ) => Page(items, total)
        )
      )

    override def get(id: ModelObjectId): AsyncCallback[Option[ModelObject]] = run(Queries.modelObject(id.value)(modelObjectSelection))

    override def upsert(entity: ModelObject): AsyncCallback[ModelObject] =
      run(
        Mutations.upsertModelObject(
          ModelObjectInput(
            id = entity.id.value,
            name = entity.name,
            description = entity.description,
            modelObjectType = toGql(entity.modelObjectType),
            deleted = entity.deleted,
            created = entity.created.toString,
            lastUpdated = entity.lastUpdated.toString
          )
        )(modelObjectSelection)
      ).flatMap {
        case Some(saved) => AsyncCallback.pure(saved)
        case None        => AsyncCallback.throwException(RepositoryError.Unexpected("The server didn't return the saved object"))
      }

    override def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): AsyncCallback[Boolean] = run(Mutations.deleteModelObject(id.value, softDelete)).map(_.getOrElse(false))

  }

  // ── Not repository operations ──────────────────────────────────────────────

  val version: AsyncCallback[String] = run(Queries.version)

}
