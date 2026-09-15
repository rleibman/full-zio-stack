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

import com.zaxxer.hikari.HikariDataSource
import net.leibman.fullziostack.db.SlickProfile.profile.api.Database
import slick.util.AsyncExecutor
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

/** Every data service, implemented with Slick. Each DB layer has an object with this name and signature, so the rest of the app doesn't depend on
  * which one is used. Add new services here.
  */
object DataLayer {

  /** Slick runs queries on its own thread pool, sized to the connection pool so it never waits for a connection. */
  private val database: URLayer[DataSource, Database] = ZLayer.scoped(
    for {
      dataSource <- ZIO.service[DataSource]
      connections = dataSource match {
        case hikari: HikariDataSource => hikari.getMaximumPoolSize
        case _ => 10
      }
      database <- ZIO.acquireRelease(
        ZIO.succeed(
          Database.forDataSource(
            dataSource,
            Some(connections),
            AsyncExecutor("slick", minThreads = connections, maxThreads = connections, queueSize = 1000, maxConnections = connections)
          )
        )
      )(database => ZIO.succeed(database.close()))
    } yield database
  )

  val live: URLayer[DataSource, ModelObjectDataService] = database >>> ZLayer.fromFunction(SlickModelObjectDataService.apply)

}
