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

package net.leibman.fullziostack.model

import java.time.Instant

// The sample entity. Every layer of the stack has a ModelObject file; to add an entity, mirror them (see the
// add-entity skill in generated projects).

enum ModelObjectType {

  case type1, type2, type3

}

opaque type ModelObjectId = Int

object ModelObjectId {

  def apply(value: Int): ModelObjectId = value

  /** The id of an entity that hasn't been saved yet. `upsert` inserts entities with this id and updates all others. */
  val empty: ModelObjectId = ModelObjectId(-1)

  extension (id: ModelObjectId) {

    def value: Int = id

  }

}

case class ModelObject(
  id:              ModelObjectId = ModelObjectId.empty,
  name:            String,
  description:     String = "",
  modelObjectType: ModelObjectType = ModelObjectType.type1,
  deleted:         Boolean = false,
  // Set by the data service on insert/update; whatever the caller passes is ignored.
  created:     Instant = Instant.EPOCH,
  lastUpdated: Instant = Instant.EPOCH
)

case class ModelObjectSearch(
  /** Case-insensitive substring of the name or description. */
  text:            Option[String] = None,
  modelObjectType: Option[ModelObjectType] = None,
  includeDeleted:  Boolean = false,
  offset:          Int = 0,
  limit:           Int = 50
)
