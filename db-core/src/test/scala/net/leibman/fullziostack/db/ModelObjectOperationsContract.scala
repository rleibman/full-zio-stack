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
import net.leibman.fullziostack.repository.CRUDOperations

/** The [[CRUDOperationsContract]] for ModelObject. Implementations extend this and provide `bootstrap`. */
abstract class ModelObjectOperationsContract extends CRUDOperationsContract[ModelObjectId, ModelObject, ModelObjectSearch] {

  override def entityName: String = "ModelObject"

  override def operations(repository: ZIORepository): CRUDOperations[DataIO, ModelObject, ModelObjectId, ModelObjectSearch] =
    repository.modelObjectOps

  override def newEntity(name: String): ModelObject =
    ModelObject(name = name, description = s"Description of $name", modelObjectType = ModelObjectType.type2)

  override def modify(entity: ModelObject): ModelObject =
    entity.copy(description = s"${entity.description} (edited)", modelObjectType = ModelObjectType.type3)

  override def idOf(entity: ModelObject): ModelObjectId = entity.id

  override def withId(
    entity: ModelObject,
    id:     ModelObjectId
  ): ModelObject = entity.copy(id = id)

  override def emptyId: ModelObjectId = ModelObjectId.empty

  override def missingId: ModelObjectId = ModelObjectId(Int.MaxValue)

  override def isDeleted(entity: ModelObject): Boolean = entity.deleted

  override def searchText(
    text:           String,
    offset:         Int,
    limit:          Int,
    includeDeleted: Boolean
  ): ModelObjectSearch = ModelObjectSearch(text = Some(text), includeDeleted = includeDeleted, offset = offset, limit = limit)

}
