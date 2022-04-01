/*
 * Copyright 2021 Roberto Leibman
 *
 * SPDX-License-Identifier: MIT
 */

package config

import com.typesafe.config.{Config, ConfigFactory}
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.config.typesafe.TypesafeConfigSource.*
import zio.logging.*

import java.io.File
import java.net.URL

case class Configuration(
  host:           String = "0.0.0.0",
  port:           Int = 8188,
  nettyThreads:   Int = 0, // When this is zero, it'll use numprocessors * 2
  maxRequestSize: Int = 210241024,
  redirectUrl:    String = "https://111.111.111.111"
)

trait ConfigurationService {

  def config: IO[ConfigurationError, Configuration]

}

object ConfigurationService {

  def configuration: ZIO[ConfigurationService, ConfigurationError, Configuration] = ZIO.serviceWithZIO(_.config)

}

case class ConfigurationError(cause: Throwable) extends Exception(cause)

object ConfigurationServiceLive {

  val layer: ULayer[ConfigurationService] = {
    ZLayer.succeed(new ConfigurationService {
      override def config: IO[ConfigurationError, Configuration] = {
        val confFileName =
          java.lang.System.getProperty("application.conf", "./src/main/resources/application.conf").nn

        val automaticDescription = descriptor[Configuration]
        val source = TypesafeConfigSource.fromHoconFilePath[Configuration](confFileName)
        val ret: IO[ReadError[K], Configuration] = read(automaticDescription from source)
        ret.mapError(ConfigurationError.apply)
      }
    })
  }

}
