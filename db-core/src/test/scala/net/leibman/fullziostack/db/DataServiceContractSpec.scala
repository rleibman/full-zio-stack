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

package net.leibman.fullziostack.db

import zio.*
import zio.test.*
import zio.test.Assertion.*

/** What every [[DataService]] must do, whatever the DB layer and database. Each entity has one subclass describing how to build and search its
  * entities (see [[ModelObjectDataServiceContract]]), and each implementation runs that subclass with its own `bootstrap` layer.
  *
  * Every test creates entities with a fresh random name, so tests don't see each other's data and the database doesn't have to be empty. That needs
  * the live Random (ZIO Test's is deterministic), and the live Clock gives the rows real timestamps.
  */
abstract class DataServiceContractSpec[PK, T, S, Service <: DataService[PK, T, S]: Tag] extends ZIOSpec[Service] {

  def entityName: String

  /** An unsaved entity (its id is the empty id) whose searchable text contains `name`. */
  def newEntity(name: String): T

  /** The same entity with a visible change to a user-editable field. */
  def modify(entity: T): T

  def idOf(entity: T): PK

  def withId(
    entity: T,
    id:     PK
  ): T

  def emptyId: PK

  /** An id no test will ever create. */
  def missingId: PK

  def isDeleted(entity: T): Boolean

  /** Searches for entities whose text contains `text`. */
  def searchText(
    text:           String,
    offset:         Int = 0,
    limit:          Int = 50,
    includeDeleted: Boolean = false
  ): S

  private val service = ZIO.service[Service]

  private val freshName = Random.nextUUID.map(uuid => s"contract-${uuid.toString.take(8)}")

  override def spec: Spec[Service & TestEnvironment & Scope, Any] =
    suite(s"$entityName data service contract")(
      test("insert assigns an id, and get returns what was saved") {
        for {
          svc   <- service
          name  <- freshName
          saved <- svc.upsert(newEntity(name))
          read  <- svc.get(idOf(saved))
        } yield assertTrue(idOf(saved) != emptyId, read.contains(saved))
      },
      test("update changes the entity and keeps its id") {
        for {
          svc     <- service
          name    <- freshName
          saved   <- svc.upsert(newEntity(name))
          updated <- svc.upsert(modify(saved))
          read    <- svc.get(idOf(saved))
        } yield assertTrue(idOf(updated) == idOf(saved), read.contains(updated), updated != saved)
      },
      test("updating an id that doesn't exist fails with NotFound") {
        for {
          svc  <- service
          name <- freshName
          exit <- svc.upsert(withId(newEntity(name), missingId)).exit
        } yield assert(exit)(fails(isSubtype[DataServiceException.NotFound](anything)))
      },
      test("get of an id that doesn't exist returns None") {
        service.flatMap(_.get(missingId)).map(read => assertTrue(read.isEmpty))
      },
      test("search pages through the matches and counts all of them") {
        for {
          svc   <- service
          name  <- freshName
          saved <- ZIO.foreach(1 to 3)(i => svc.upsert(newEntity(s"$name-$i")))
          first <- svc.search(searchText(name, offset = 0, limit = 2))
          rest  <- svc.search(searchText(name, offset = 2, limit = 2))
        } yield assertTrue(
          first.total == 3L,
          rest.total == 3L,
          first.items.size == 2,
          rest.items.size == 1,
          (first.items ++ rest.items).map(idOf).toSet == saved.map(idOf).toSet
        )
      },
      test("soft delete hides the entity from search, but get still returns it, flagged") {
        for {
          svc      <- service
          name     <- freshName
          saved    <- svc.upsert(newEntity(name))
          deleted  <- svc.delete(idOf(saved), softDelete = true)
          visible  <- svc.search(searchText(name))
          included <- svc.search(searchText(name, includeDeleted = true))
          read     <- svc.get(idOf(saved))
        } yield assertTrue(
          deleted,
          visible.total == 0L,
          included.items.map(idOf) == Seq(idOf(saved)),
          read.exists(isDeleted)
        )
      },
      test("hard delete removes the entity; deleting it again returns false") {
        for {
          svc    <- service
          name   <- freshName
          saved  <- svc.upsert(newEntity(name))
          first  <- svc.delete(idOf(saved), softDelete = false)
          second <- svc.delete(idOf(saved), softDelete = false)
          read   <- svc.get(idOf(saved))
        } yield assertTrue(first, !second, read.isEmpty)
      }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ TestAspect.withLiveRandom

}
