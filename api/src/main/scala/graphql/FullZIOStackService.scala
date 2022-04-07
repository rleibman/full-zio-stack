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

    def all: UIO[List[ModelObject]]

    def search: UIO[List[ModelObject]]

    def get(id: ModelObjectId): UIO[Option[ModelObject]]

    def delete(id: ModelObjectId): UIO[Boolean]

    def upsert(modelObject: ModelObject): UIO[ModelObject]

  }
  def all: URIO[FullZIOStackService, List[ModelObject]] = URIO.serviceWithZIO(_.all)

  def search: URIO[FullZIOStackService, List[ModelObject]] = URIO.serviceWithZIO(_.search)

  def get(id: ModelObjectId): URIO[FullZIOStackService, Option[ModelObject]] = URIO.serviceWithZIO(_.get(id))

  def delete(id: ModelObjectId): URIO[FullZIOStackService, Boolean] = URIO.serviceWithZIO(_.delete(id))

  def upsert(modelObject: ModelObject): URIO[FullZIOStackService, ModelObject] = URIO.serviceWithZIO(_.upsert(modelObject))

  def make: ZLayer[ModelObjectDataService, Nothing, FullZIOStackService] =
    (for {
      ds <- ZIO.service[ModelObjectDataService]
    } yield new FullZIOStackService {

      override def all: UIO[List[ModelObject]] = ds.search(None).orDie

      override def search: UIO[List[ModelObject]] = ds.search(None).orDie

      override def get(id: ModelObjectId): UIO[Option[ModelObject]] = ds.get(id).orDie

      override def delete(id: ModelObjectId): UIO[Boolean] = ds.delete(id, true).orDie

      override def upsert(modelObject: ModelObject): UIO[ModelObject] = ds.upsert(modelObject).orDie

    }).toLayer

}
