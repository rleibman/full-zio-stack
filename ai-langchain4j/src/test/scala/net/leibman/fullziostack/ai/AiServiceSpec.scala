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
import net.leibman.fullziostack.config.AiConfig
import zio.*
import zio.test.*
import zio.test.Assertion.*

/** The AI service, against a stub model: no provider is called. */
object AiServiceSpec extends ZIOSpecDefault {

  /** Answers with the prompt it was given, so the test can look at it. */
  private val echoModel: ChatModel = new ChatModel {
    override def chat(userMessage: String): String = s"Echo: $userMessage"
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("AiService")(
      test("asks the model for a one-sentence description of the name") {
        for {
          answer <- Langchain4jAiService(echoModel).suggestDescription("Blue widget")
        } yield assertTrue(answer.startsWith("Echo:"), answer.contains("Blue widget"), answer.contains("one sentence"))
      },
      test("without a provider, every call says so") {
        for {
          service <- ZIO.service[AiService].provide(ZLayer.succeed(AiConfig()) >>> AiService.live)
          failure <- service.suggestDescription("Blue widget").exit
        } yield assert(failure)(fails(isSubtype[AiError.NotConfigured](anything)))
      },
      test("a provider that can't be built (no API key, say) leaves the server running, with AI off") {
        for {
          // anthropic without an API key: langchain4j throws while building the model.
          service <- ZIO.service[AiService].provide(ZLayer.succeed(AiConfig(provider = "anthropic", model = "claude-sonnet-5")) >>> AiService.live)
          failure <- service.suggestDescription("Blue widget").exit
        } yield assert(failure)(fails(isSubtype[AiError.NotConfigured](anything)))
      },
      test("an unknown provider is reported as not configured") {
        for {
          service <- ZIO.service[AiService].provide(ZLayer.succeed(AiConfig(provider = "hal9000")) >>> AiService.live)
          failure <- service.suggestDescription("Blue widget").exit
        } yield assert(failure)(fails(isSubtype[AiError.NotConfigured](anything)))
      }
    )

}
