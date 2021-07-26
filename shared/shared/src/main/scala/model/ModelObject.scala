/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package model

import java.time.LocalDateTime

enum ModelObjectType {
  case type1
  case type2
  case type3
}

opaque type ModelObjectId = Int

object ModelObjectId {
  def apply(value: Int): ModelObjectId = value
}

extension (id: ModelObjectId) {
  def toInt: Int = id
}

case class ModelObject(
  id:          ModelObjectId,
  name:        String,
  description: String,
  deleted:     Boolean = false,
  lastUpdated: LocalDateTime,
  created:     LocalDateTime,
  modelObjectType: ModelObjectType,
)

val me = ModelObjectId(1)
