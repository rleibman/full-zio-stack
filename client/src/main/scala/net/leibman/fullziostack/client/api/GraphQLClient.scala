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

package net.leibman.fullziostack.client.api

import caliban.client.CalibanClientError.ServerError
import caliban.client.Operations.IsOperation
import caliban.client.SelectionBuilder
import japgolly.scalajs.react.AsyncCallback
import org.scalajs.dom
import sttp.client4.*
import sttp.client4.fetch.FetchBackend

import scala.concurrent.ExecutionContext.Implicits.global

/** Runs operations built with the generated [[FullZIOStackClient]] against the server this page was loaded from. */
object GraphQLClient {

  private val backend = FetchBackend()

  lazy private val endpoint = uri"${dom.window.location.origin}/api/graphql"

  /** Fails with a [[GraphQLClientError]] whose message is fit to show to users. */
  def run[Origin: IsOperation, A](selection: SelectionBuilder[Origin, A]): AsyncCallback[A] =
    AsyncCallback
      .fromFuture(selection.toRequest(endpoint).send(backend))
      .flatMap(response =>
        response.body match {
          case Right(result)             => AsyncCallback.pure(result)
          case Left(ServerError(errors)) => AsyncCallback.throwException(GraphQLClientError(errors.map(_.message).mkString("; ")))
          case Left(error)               => AsyncCallback.throwException(GraphQLClientError(s"Couldn't reach the server (${error.getMessage})"))
        }
      )

}

final case class GraphQLClientError(message: String) extends Exception(message)
