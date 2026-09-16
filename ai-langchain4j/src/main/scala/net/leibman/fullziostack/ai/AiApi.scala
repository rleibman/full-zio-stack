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

package net.leibman.fullziostack.ai

import caliban.*
import caliban.CalibanError.ExecutionError
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import caliban.schema.*
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Annotations.GQLDescription
import zio.*

/** The AI part of the GraphQL API. The server combines it with the rest (see ApiDefinition). */
object AiApi {

  object AiSchema extends GenericSchema[AiService]
  import AiSchema.auto.*

  case class SuggestDescriptionArgs(name: String)

  case class Mutations(
    @GQLDescription("Suggests a description for something with this name, using the configured AI provider")
    suggestDescription: SuggestDescriptionArgs => ZIO[AiService, ExecutionError, String]
  )

  given Schema[AiService, Mutations] = AiSchema.gen[AiService, Mutations]

  private def error(
    code:    String,
    message: String
  ): ExecutionError =
    ExecutionError(message, extensions = Some(ObjectValue(List("code" -> StringValue(code)))))

  private def suggest(name: String): ZIO[AiService, ExecutionError, String] =
    if (name.isBlank) ZIO.fail(error("BAD_USER_INPUT", "name must not be blank"))
    else
      ZIO
        .serviceWithZIO[AiService](_.suggestDescription(name.trim.nn))
        .mapError {
          case e: AiError.NotConfigured => error("UNAVAILABLE", e.getMessage)
          case e: AiError.Unavailable   => error("UNAVAILABLE", "The AI provider is unavailable, try again")
        }

  val api: GraphQL[AiService] = graphQL(RootResolver(Option.empty[Unit], Mutations(args => suggest(args.name))))

}
