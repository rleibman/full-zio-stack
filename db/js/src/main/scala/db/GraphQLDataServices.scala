/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package db

import model.*
import zio.{IO, ULayer, ZLayer}

object GraphQLDataServices {

  final private case class GraphQLModelObjectDataService(
    host: String,
    port: Int
  ) extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, IndexedSeq[ModelObject]] = ???

    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] = ???

    def get(id: ModelObjectId): zio.IO[DataServiceException, Option[ModelObject]] = ???

    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] = ???

  }

  def modelObjectDataServices(
    host: String,
    port: Int
  ): ULayer[ModelObjectDataService] = ZLayer.succeed(GraphQLModelObjectDataService(host, port))

}
