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

package net.leibman.fullziostack.config

/** How to reach a large language model. Lives in the shared model so both the server's configuration and the `ai`
  * module can use it.
  *
  * @param provider
  *   `anthropic`, `openai`, `ollama`, or `none` (the default: AI features report that they aren't configured)
  * @param model
  *   the model id, e.g. `claude-sonnet-5`, `gpt-4o-mini`, `llama3.2`
  * @param apiKey
  *   the provider's API key; keep it out of the file and set AI_API_KEY instead
  * @param baseUrl
  *   overrides the provider's endpoint (Ollama's, say, or a proxy)
  */
case class AiConfig(
  provider:       String = "none",
  model:          String = "",
  apiKey:         Option[String] = None,
  baseUrl:        Option[String] = None,
  timeoutSeconds: Int = 60
)
