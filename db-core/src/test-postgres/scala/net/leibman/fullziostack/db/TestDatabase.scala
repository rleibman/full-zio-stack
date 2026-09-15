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

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.*

import javax.sql.DataSource

/** A throwaway PostgreSQL in a container, with the migrations applied. Needs Docker. */
object TestDatabase {

  val config: TaskLayer[DatabaseConfig] = ZLayer.scoped {
    ZIO
      .acquireRelease(ZIO.attemptBlocking {
        val image: DockerImageName = DockerImageName.parse("postgres:17").nn
        val container = new PostgreSQLContainer(dockerImageNameOverride = Some(image))
        container.start()
        container
      })(container => ZIO.attemptBlocking(container.stop()).orDie)
      .map(container =>
        DatabaseConfig(
          url = container.jdbcUrl,
          user = container.username,
          password = container.password,
          migrationsLocation = "classpath:db/migration/postgres"
        )
      )
  }

  val dataSource: TaskLayer[DataSource] = config >>> DataSources.live

}
