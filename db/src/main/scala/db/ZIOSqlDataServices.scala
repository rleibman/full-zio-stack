/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package db

import model.*
import zio.*

import javax.sql.DataSource

object ZIOSqlDataServices {

  final private case class ZIOSqlModelObjectDataService(dataSource: DataSource) extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, List[ModelObject]] = ???
    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] = ???
    def get(id: ModelObjectId):   zio.IO[DataServiceException, Option[ModelObject]] = ???
    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] = ???

  }

  val modelObjectDataServices: URLayer[DataSource, ModelObjectDataService] = (ZIOSqlModelObjectDataService.apply _).toLayer[ModelObjectDataService]

}
