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

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import zio.*

import java.nio.file.{Files, Path}
import javax.sql.DataSource

case class DatabaseConfig(
  url:      String,
  user:     String = "",
  password: String = "",
  /** Flyway location of this database's migrations, e.g. `classpath:db/migration/mysql`. */
  migrationsLocation:      String,
  maximumPoolSize:         Int = 10,
  minimumIdle:             Int = 1,
  connectionTimeoutMillis: Long = 30000,
  /** SQL run on every new connection. (SQLite takes its pragmas as URL parameters instead, e.g.
    * `jdbc:sqlite:data/app.db?journal_mode=WAL&busy_timeout=5000`.)
    */
  connectionInitSql: Option[String] = None
)

object DataSources {

  /** A pooled DataSource with every migration applied. The pool is closed when the layer's scope ends. */
  val live: RLayer[DatabaseConfig, DataSource] = ZLayer.scoped {
    for {
      config     <- ZIO.service[DatabaseConfig]
      _          <- ZIO.attemptBlocking(createSqliteDirectory(config.url))
      dataSource <- ZIO.acquireRelease(ZIO.attemptBlocking(hikari(config)))(ds => ZIO.attemptBlocking(ds.close()).orDie)
      _          <- migrate(config, dataSource)
    } yield dataSource
  }

  /** SQLite creates the database file on first use, but not the directory it lives in. */
  private def createSqliteDirectory(url: String): Unit =
    if (url.startsWith("jdbc:sqlite:")) {
      val file = url.stripPrefix("jdbc:sqlite:").takeWhile(_ != '?')
      if (file.nonEmpty && !file.startsWith(":memory:"))
        Option(Path.of(file).nn.toAbsolutePath.nn.getParent).foreach(directory => Files.createDirectories(directory))
    }

  private def hikari(config: DatabaseConfig): HikariDataSource = {
    val hikariConfig = HikariConfig()
    hikariConfig.setJdbcUrl(config.url)
    hikariConfig.setUsername(config.user)
    hikariConfig.setPassword(config.password)
    hikariConfig.setMaximumPoolSize(config.maximumPoolSize)
    hikariConfig.setMinimumIdle(config.minimumIdle)
    hikariConfig.setConnectionTimeout(config.connectionTimeoutMillis)
    config.connectionInitSql.foreach(hikariConfig.setConnectionInitSql)
    HikariDataSource(hikariConfig)
  }

  private def migrate(
    config:     DatabaseConfig,
    dataSource: DataSource
  ): Task[Unit] =
    for {
      result <- ZIO.attemptBlocking(
        Flyway.configure().nn.dataSource(dataSource).nn.locations(config.migrationsLocation).nn.load().nn.migrate().nn
      )
      _ <- ZIO.logInfo(s"Flyway applied ${result.migrationsExecuted} migration(s) from ${config.migrationsLocation}")
    } yield ()

}
