/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package graphql

import caliban.CalibanError
import db.ModelObjectDataService
import zhttp.http.HttpApp
import zio.{IO, ZIO}

object FullZIOStackHttp {

  val route = (for {
    interpreter <- FullZIOStackApi.api.interpreter.provideLayer(FullZIOStackService.make)
  } yield caliban.ZHttpAdapter.makeHttpService(interpreter))

}
