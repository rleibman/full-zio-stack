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

    def all: UIO[IndexedSeq[ModelObject]]

    def search: UIO[IndexedSeq[ModelObject]]

    def get(id: ModelObjectId): UIO[Option[ModelObject]]

    def delete(id: ModelObjectId): UIO[Boolean]

    def upsert(modelObject: ModelObject): UIO[ModelObject]

  }
  def all: URIO[FullZIOStackService, IndexedSeq[ModelObject]] = ZIO.serviceWithZIO(_.all)

  def search: URIO[FullZIOStackService, IndexedSeq[ModelObject]] = ZIO.serviceWithZIO(_.search)

  def get(id: ModelObjectId): URIO[FullZIOStackService, Option[ModelObject]] = ZIO.serviceWithZIO(_.get(id))

  def delete(id: ModelObjectId): URIO[FullZIOStackService, Boolean] = ZIO.serviceWithZIO(_.delete(id))

  def upsert(modelObject: ModelObject): URIO[FullZIOStackService, ModelObject] = ZIO.serviceWithZIO(_.upsert(modelObject))

  def make: ZLayer[ModelObjectDataService, Nothing, FullZIOStackService] =
    ZLayer(for {
      ds <- ZIO.service[ModelObjectDataService]
    } yield new FullZIOStackService {

      override def all: UIO[IndexedSeq[ModelObject]] = ds.search(None).orDie

      override def search: UIO[IndexedSeq[ModelObject]] = ds.search(None).orDie

      override def get(id: ModelObjectId): UIO[Option[ModelObject]] = ds.get(id).orDie

      override def delete(id: ModelObjectId): UIO[Boolean] = ds.delete(id, true).orDie

      override def upsert(modelObject: ModelObject): UIO[ModelObject] = ds.upsert(modelObject).orDie

    })

}
