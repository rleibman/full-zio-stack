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

package net.leibman.fullziostack.client.api

import caliban.client.SelectionBuilder
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

import java.time.Instant

/** The ModelObject operations of the GraphQL API, in terms of the shared model. */
object ModelObjectApi {

  private def fromGql(modelObjectType: GqlModelObjectType): ModelObjectType = ModelObjectType.valueOf(modelObjectType.value)

  private def toGql(modelObjectType: ModelObjectType): GqlModelObjectType =
    GqlModelObjectType.values.find(_.value == modelObjectType.toString).getOrElse(GqlModelObjectType.type1)

  private val modelObject: SelectionBuilder[GqlModelObject, ModelObject] =
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

  def search(search: ModelObjectSearch): AsyncCallback[Page[ModelObject]] =
    GraphQLClient
      .run(
        Queries.modelObjects(
          ModelObjectSearchInput(
            text = search.text,
            modelObjectType = search.modelObjectType.map(toGql),
            includeDeleted = search.includeDeleted,
            offset = search.offset,
            limit = search.limit
          )
        )(PageModelObject.items(modelObject) ~ PageModelObject.total)
      )
      .map(
        _.fold(Page(Seq.empty[ModelObject], 0L))(
          (
            items,
            total
          ) => Page(items, total)
        )
      )

  def upsert(obj: ModelObject): AsyncCallback[ModelObject] =
    GraphQLClient
      .run(
        Mutations.upsertModelObject(
          ModelObjectInput(
            id = obj.id.value,
            name = obj.name,
            description = obj.description,
            modelObjectType = toGql(obj.modelObjectType),
            deleted = obj.deleted,
            created = obj.created.toString,
            lastUpdated = obj.lastUpdated.toString
          )
        )(modelObject)
      )
      .flatMap {
        case Some(saved) => AsyncCallback.pure(saved)
        case None        => AsyncCallback.throwException(GraphQLClientError("The server didn't return the saved object"))
      }

  def delete(
    id:         ModelObjectId,
    softDelete: Boolean
  ): AsyncCallback[Boolean] =
    GraphQLClient.run(Mutations.deleteModelObject(id.value, softDelete)).map(_.getOrElse(false))

  val version: AsyncCallback[String] = GraphQLClient.run(Queries.version)

}
