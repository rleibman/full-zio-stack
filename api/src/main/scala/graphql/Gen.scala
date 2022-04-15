/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package graphql

object Gen extends App {
  println(FullZIOStackApi.api.render)
}
