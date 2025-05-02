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

package graphql

import config.ConfigurationService
import db.{DataServiceException, ModelObjectDataService, DBIO}
import model.*
import zio.*

trait FullZIOStackService {

  def all: UIO[IndexedSeq[ModelObject]]

  def search: UIO[IndexedSeq[ModelObject]]

  def get(id: ModelObjectId): UIO[Option[ModelObject]]

  def delete(id: ModelObjectId): UIO[Boolean]

  def upsert(modelObject: ModelObject): UIO[ModelObject]

}

object FullZIOStackService {

  def all: URIO[FullZIOStackService, IndexedSeq[ModelObject]] = ZIO.serviceWithZIO(_.all)

  def search: URIO[FullZIOStackService, IndexedSeq[ModelObject]] = ZIO.serviceWithZIO(_.search)

  def get(id: ModelObjectId): URIO[FullZIOStackService, Option[ModelObject]] = ZIO.serviceWithZIO(_.get(id))

  def delete(id: ModelObjectId): URIO[FullZIOStackService, Boolean] = ZIO.serviceWithZIO(_.delete(id))

  def upsert(modelObject: ModelObject): URIO[FullZIOStackService, ModelObject] = ZIO.serviceWithZIO(_.upsert(modelObject))

  def live: ZLayer[ModelObjectDataService[DBIO], Nothing, FullZIOStackService] =
    ZLayer(for {
      ds <- ZIO.service[ModelObjectDataService[DBIO]]
    } yield new FullZIOStackService {

      override def all: UIO[IndexedSeq[ModelObject]] = ds.search(None).orDie

      override def search: UIO[IndexedSeq[ModelObject]] = ds.search(None).orDie

      override def get(id: ModelObjectId): UIO[Option[ModelObject]] = ds.get(id).orDie

      override def delete(id: ModelObjectId): UIO[Boolean] = ds.delete(id, true).orDie

      override def upsert(modelObject: ModelObject): UIO[ModelObject] = ds.upsert(modelObject).orDie

    })

}
