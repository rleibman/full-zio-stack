/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package db

import model.*
import model.given
import zio.{IO, ULayer, ZIO, ZLayer}
import zio.http.*
import zio.json.*

object RESTDataServices {

  final private case class RESTModelObjectDataService(
    host: String,
    port: Int
  ) extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, IndexedSeq[ModelObject]] =
      (for {
        res    <- Client.request("/modelObjects", headers = Headers.accept(HeaderValues.applicationJson))
        str    <- res.bodyAsString
        parsed <- ZIO.fromEither(str.fromJson[IndexedSeq[ModelObject]]).mapError(new DataServiceException(_, None))
      } yield parsed).provideLayer(ChannelFactory.auto ++ EventLoopGroup.auto()).mapError(DataServiceException(_))

    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] =
      (for {
        res    <- Client.request(s"/modelObject/${id.asInt}", Method.DELETE, headers = Headers.accept(HeaderValues.applicationJson))
        str    <- res.bodyAsString
        parsed <- ZIO.fromEither(str.fromJson[Boolean]).mapError(new DataServiceException(_, None))
      } yield parsed).provideLayer(ChannelFactory.auto ++ EventLoopGroup.auto()).mapError(DataServiceException(_))

    def get(id: ModelObjectId): zio.IO[DataServiceException, Option[ModelObject]] =
      (for {
        res    <- Client.request(s"/modelObject/${id.asInt}", headers = Headers.accept(HeaderValues.applicationJson))
        str    <- res.bodyAsString
        parsed <- ZIO.fromEither(str.fromJson[Option[ModelObject]]).mapError(new DataServiceException(_, None))
      } yield parsed).provideLayer(ChannelFactory.auto ++ EventLoopGroup.auto()).mapError(DataServiceException(_))

    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] =
      (for {
        res <- Client.request(
          url = s"/modelObject",
          method = Method.PUT,
          headers = Headers.accept(HeaderValues.applicationJson),
          content = HttpData.fromString(obj.toJson)
        )
        str    <- res.bodyAsString
        parsed <- ZIO.fromEither(str.fromJson[ModelObject]).mapError(new DataServiceException(_, None))
      } yield parsed).provideLayer(ChannelFactory.auto ++ EventLoopGroup.auto()).mapError(DataServiceException(_))

  }

  def modelObjectDataServices(
    host: String,
    port: Int
  ): ULayer[ModelObjectDataService] = ZLayer.succeed(RESTModelObjectDataService(host, port))

}
