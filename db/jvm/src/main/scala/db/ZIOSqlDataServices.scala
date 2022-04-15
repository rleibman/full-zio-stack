/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package db

import model.*
import zio.{IO, URLayer, ZIO, ZLayer}

import javax.sql.DataSource

object ZIOSqlDataServices {

  final private case class ZIOSqlModelObjectDataService(dataSource: DataSource) extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, IndexedSeq[ModelObject]] = ???
    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] = ???
    def get(id: ModelObjectId):   zio.IO[DataServiceException, Option[ModelObject]] = ???
    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] = ???

  }

  val modelObjectDataServices: URLayer[DataSource, ModelObjectDataService] = ZLayer.fromZIO(for {
    ds <- ZIO.service[DataSource]
  } yield ZIOSqlModelObjectDataService(ds))

}
