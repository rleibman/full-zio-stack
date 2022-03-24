/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package japgolly.scalajs.react.zioEffect

import japgolly.scalajs.react._

import zio.*

extension [ A](z: UIO[A]) def toAsyncCallback: AsyncCallback[A] = AsyncCallback[A](_ => zio.Runtime.default.unsafeRunToFuture(z))

//extension [ A](async: AsyncCallback[A]) def toZIO: UIO[A] =  ZIO(async.a)
