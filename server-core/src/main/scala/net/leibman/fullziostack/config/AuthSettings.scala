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

package net.leibman.fullziostack.config

/** Only used by projects generated with authentication; harmless otherwise.
  *
  * @param secret
  *   signs the session tokens. Anyone who knows it can mint a session for any user, so it belongs in the environment
  *   (`AUTH_SECRET`), not in a file, and changing it logs everyone out.
  * @param webHostUrl
  *   where the client is reachable, used to build the links in registration and password-recovery emails.
  * @param sessionMinutes
  *   how long an access token is good for; the client refreshes it with the longer-lived refresh cookie.
  * @param secureCookie
  *   whether the refresh cookie is HTTPS-only. Production should leave it true; it has to be false to log in over
  *   plain http, which is why development sets it so.
  */
case class AuthSettings(
  secret:         String = "",
  webHostUrl:     String = "http://localhost:8080",
  sessionMinutes: Int = 60,
  refreshMinutes: Int = 1440,
  secureCookie:   Boolean = true
)
