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

import zio.json.*

// JSON codecs for the model. The GraphQL API derives its own schema; these are for anything else that speaks JSON.

given JsonCodec[ModelObjectId] = JsonCodec.int.transform(ModelObjectId.apply, _.value)

given JsonCodec[ModelObjectType] =
  JsonCodec.string.transformOrFail(
    s => ModelObjectType.values.find(_.toString == s).toRight(s"Unknown ModelObjectType: $s"),
    _.toString
  )

given JsonCodec[ModelObject] = DeriveJsonCodec.gen[ModelObject]

given JsonCodec[ModelObjectSearch] = DeriveJsonCodec.gen[ModelObjectSearch]

given [T: JsonCodec]: JsonCodec[Page[T]] = DeriveJsonCodec.gen[Page[T]]
