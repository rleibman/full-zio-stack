/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package graphql

import config.ConfigurationService
import db.{DataServiceException, ModelObjectDataService}
import model.*
import zio.*

object FullZIOStackService {

  trait FullZIOStackService {

    def all: Task[List[ModelObject]]

    def search: Task[List[ModelObject]]

    def get(id: ModelObjectId): Task[Option[ModelObject]]

    def delete(id: ModelObjectId): Task[Boolean]

    def upsert(modelObject: ModelObject): Task[ModelObject]

  }
  def all: RIO[FullZIOStackService, List[ModelObject]] = RIO.serviceWithZIO(_.all)

  def search: RIO[FullZIOStackService, List[ModelObject]] = RIO.serviceWithZIO(_.search)

  def get(id: ModelObjectId): RIO[FullZIOStackService, Option[ModelObject]] = RIO.serviceWithZIO(_.get(id))

  def delete(id: ModelObjectId): RIO[FullZIOStackService, Boolean] = RIO.serviceWithZIO(_.delete(id))

  def upsert(modelObject: ModelObject): RIO[FullZIOStackService, ModelObject] = RIO.serviceWithZIO(_.upsert(modelObject))

  def make: ZLayer[ModelObjectDataService, Nothing, FullZIOStackService] =
    (for {
      ds <- ZIO.service[ModelObjectDataService]
    } yield new FullZIOStackService {

      override def all: Task[List[ModelObject]] = ds.search(None)

      override def search: Task[List[ModelObject]] = ds.search(None)

      override def get(id: ModelObjectId): Task[Option[ModelObject]] = ds.get(id)

      override def delete(id: ModelObjectId): Task[Boolean] = ds.delete(id, true)

      override def upsert(modelObject: ModelObject): Task[ModelObject] = ds.upsert(modelObject)

    }).toLayer

}
