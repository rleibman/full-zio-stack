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

import dev.langchain4j.model.chat.ChatModel
import zio.*

/** [[AiService]] over a langchain4j model. Calls block, so they run on ZIO's blocking pool. */
final case class Langchain4jAiService(model: ChatModel) extends AiService {

  override def suggestDescription(name: String): IO[AiError, String] =
    chat(
      s"""Write one sentence, at most 140 characters, describing an item called "$name".
         |Answer with the sentence itself, nothing else.""".stripMargin
    ).map(_.trim.nn)

  private def chat(prompt: String): IO[AiError, String] =
    ZIO
      .attemptBlocking(model.chat(prompt).nn)
      .tapErrorCause(cause => ZIO.logWarningCause("The AI provider failed", cause))
      .mapError(e => AiError.Unavailable(Option(e.getMessage).getOrElse("The AI provider failed"), Some(e)))

}
