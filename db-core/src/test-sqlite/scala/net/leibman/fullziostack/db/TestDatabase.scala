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

import zio.*

import java.nio.file.{Files, Path}
import javax.sql.DataSource

/** A throwaway SQLite database in a temporary directory, with the migrations applied. A file rather than `:memory:`, because every connection to
  * `:memory:` gets its own empty database.
  */
object TestDatabase {

  val config: TaskLayer[DatabaseConfig] = ZLayer.scoped {
    ZIO
      .acquireRelease(ZIO.attemptBlocking[Path](Files.createTempDirectory("sqlite-test").nn))(directory =>
        ZIO
          .attemptBlocking(Files.list(directory).nn.forEach(file => Files.delete(file)): Unit).zipRight(
            ZIO.attemptBlocking(Files.delete(directory))
          ).orDie
      )
      .map(directory =>
        DatabaseConfig(
          url = s"jdbc:sqlite:${directory.resolve("test.db")}?journal_mode=WAL&busy_timeout=5000",
          migrationsLocation = "classpath:db/migration/sqlite",
          maximumPoolSize = 1
        )
      )
  }

  val dataSource: TaskLayer[DataSource] = config >>> DataSources.live

}
