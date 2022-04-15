/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package db

import model.*
import zio.*

object MockDataServices {

  final private case class MockModelObjectDataService() extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, IndexedSeq[ModelObject]] = ???

    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] = ???

    def get(id: ModelObjectId): zio.IO[DataServiceException, Option[ModelObject]] = ???

    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] = ???

  }

  val modelObjectDataServices: ULayer[ModelObjectDataService] = ZLayer.succeed(MockModelObjectDataService())

}
