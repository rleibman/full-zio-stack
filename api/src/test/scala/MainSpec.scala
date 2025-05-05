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

import Main.Environment
import zio.*
import zio.http.*
import zio.internal.stacktracer.Tracer
import zio.test.Assertion.*
import zio.test.{ZIOSpec, *}

object MainSpec extends ZIOSpec[Main.Environment] {

  override val bootstrap = EnvironmentBuilder.mock

  override def spec =
    suite("http")(
      test("should be ok") {
        val req = URL.decode("/text").map(s => Request(url = s)).getOrElse(Request())
        for {
          app <- Main.zapp
          one <- app(req)
        } yield assertTrue(one.status == Status.Ok)
      },
      test("Running two tests against the same app") {
        val req = URL.decode("/text").map(s => Request(url = s)).getOrElse(Request())

        for {
          app <- Main.zapp
          one <- app(req)
          two <- app(req)
        } yield assert(one.status)(equalTo(Status.Ok)) && assert(two.status)(equalTo(Status.Ok))
      }
    )

}
