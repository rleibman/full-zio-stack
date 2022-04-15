/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import zio.json.*

package object model {
  given JsonDecoder[ModelObjectId] = JsonDecoder.int.map(ModelObjectId.apply)

  given JsonDecoder[ModelObjectType] = JsonDecoder.string.map(ModelObjectType.valueOf)

  given JsonEncoder[ModelObjectId] = JsonEncoder.int.contramap[ModelObjectId](_.asInt)

  given JsonEncoder[ModelObjectType] = JsonEncoder.string.contramap[ModelObjectType](_.toString)

  given JsonDecoder[ModelObject] = DeriveJsonDecoder.gen[ModelObject]

  given JsonEncoder[ModelObject] = DeriveJsonEncoder.gen[ModelObject]

}
