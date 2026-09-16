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

import caliban.GraphQL
import net.leibman.fullziostack.db.ZIORepository
import zio.telemetry.opentelemetry.tracing.Tracing

/** The GraphQL API the server serves: the application's own, plus whatever the optional features add.
  *
  * Each optional feature has a module object with the same name in one file per choice (AiModule, and AuthModule in
  * projects generated with authentication), so features compose without a file per combination.
  */
object ApiDefinition {

  /** Everything the API needs: the repository and tracing, plus the optional features' environments (`Any` when a
    * feature is off, which adds nothing to the intersection).
    */
  type ApiEnvironment = ZIORepository & Tracing & AiModule.Env

  val api: GraphQL[ApiEnvironment] =
    AiModule.apis.foldLeft[GraphQL[ApiEnvironment]](FullZIOStackApi.api)(_ |+| _)

  /** Layers the optional features need, built from the application's configuration. */
  val extraLayers = AiModule.layers

}
