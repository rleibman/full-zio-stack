/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

import io.getquill.*
import io.getquill.context.ZioJdbc.*
import model.ModelObject
import zio.*
import zio.console.putStrLn

import java.io.Closeable
import java.sql.SQLException
import javax.sql.DataSource

package object db {
  object QuillContext extends MysqlZioJdbcContext(SnakeCase) {
    val dataSourceLayer: ULayer[Has[DataSource & Closeable]] =
      DataSourceLayer.fromPrefix("database").orDie
  }
  trait DataService {
    def getModelObjects: IO[SQLException, List[ModelObject]]
  }

  object DataService {
    val live = (DataServiceLive.apply _).toLayer[DataService]
  }

  import QuillContext.*
  final case class DataServiceLive(dataSource: DataSource & Closeable) extends DataService {
    val env: Has[DataSource & Closeable] = Has(dataSource)
    override def getModelObjects: IO[SQLException, List[ModelObject]] = run(query[ModelObject]).provide(env)
  }

}
