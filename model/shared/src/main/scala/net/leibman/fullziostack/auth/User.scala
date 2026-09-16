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

package net.leibman.fullziostack.auth

import zio.json.*

import java.time.Instant

// The account a person logs in with. Only in projects generated with authentication; zio-auth turns these into
// sessions, and the client shows the logged-in user. Passwords are never part of this type: the server keeps their
// hashes in the database and nothing but UserStore ever sees them.

opaque type UserId = Int

object UserId {

  def apply(value: Int): UserId = value

  /** The id of a user who hasn't been saved yet. */
  val empty: UserId = UserId(-1)

  given JsonCodec[UserId] = JsonCodec[Int]

  given CanEqual[UserId, UserId] = CanEqual.derived

  extension (id: UserId) {

    def value: Int = id

  }

}

/** Identifies one client connection of one user, so a user logged in twice can be told apart (a websocket, a browser
  * tab). zio-auth carries it in the session; nothing in the generated application uses it yet.
  */
opaque type ConnectionId = String

object ConnectionId {

  def apply(value: String): ConnectionId = value

  def random: ConnectionId = ConnectionId(java.util.UUID.randomUUID().nn.toString)

  given JsonCodec[ConnectionId] = JsonCodec[String]

  given CanEqual[ConnectionId, ConnectionId] = CanEqual.derived

  extension (id: ConnectionId) {

    def value: String = id

  }

}

case class User(
  id:     UserId = UserId.empty,
  email:  String,
  name:   String,
  /** Set when the user confirms the registration email. An inactive user can't log in. */
  active: Boolean = false,
  // Set by UserStore on insert/update; whatever the caller passes is ignored.
  created: Instant = Instant.EPOCH,
  deleted: Boolean = false
) derives JsonCodec
