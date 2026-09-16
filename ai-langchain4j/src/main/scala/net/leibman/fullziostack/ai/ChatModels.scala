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

import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import net.leibman.fullziostack.config.AiConfig

import java.time.Duration

/** Builds the langchain4j model for the configured provider. Add a provider here and to its dependency in build.sbt. */
object ChatModels {

  def create(config: AiConfig): Option[ChatModel] = {
    val timeout = Duration.ofSeconds(config.timeoutSeconds.toLong).nn
    config.provider.toLowerCase match {
      case "anthropic" =>
        val builder = AnthropicChatModel.builder().nn.modelName(config.model).nn.timeout(timeout).nn
        config.apiKey.foreach(builder.apiKey)
        config.baseUrl.foreach(builder.baseUrl)
        Some(builder.build().nn)
      case "openai" =>
        val builder = OpenAiChatModel.builder().nn.modelName(config.model).nn.timeout(timeout).nn
        config.apiKey.foreach(builder.apiKey)
        config.baseUrl.foreach(builder.baseUrl)
        Some(builder.build().nn)
      case "ollama" =>
        val builder = OllamaChatModel.builder().nn.modelName(config.model).nn.timeout(timeout).nn
        builder.baseUrl(config.baseUrl.getOrElse("http://localhost:11434"))
        Some(builder.build().nn)
      case _ => None
    }
  }

}
