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

package net.leibman.fullziostack.db

import doobie.Transactor
import zio.*
import zio.interop.catz.*

import javax.sql.DataSource

/** Every data service, implemented with doobie. Each DB layer has an object with this name and signature, so the rest of the app doesn't depend on
  * which one is used. Add new services here.
  */
object DataLayer {

  /** JDBC blocks, so doobie gets ZIO's blocking executor for its connections. */
  private val transactor: URLayer[DataSource, Transactor[Task]] = ZLayer.fromZIO(
    for {
      dataSource <- ZIO.service[DataSource]
      blocking   <- ZIO.blockingExecutor
    } yield Transactor.fromDataSource[Task](dataSource, blocking.asExecutionContext)
  )

  val live: URLayer[DataSource, ModelObjectDataService] = transactor >>> ZLayer.fromFunction(DoobieModelObjectDataService.apply)

}
