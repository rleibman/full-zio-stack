package net.leibman.fullziostack.graphql

import caliban.GraphQL
import net.leibman.fullziostack.ai.{AiApi, AiService}
import net.leibman.fullziostack.config.AppConfig
import zio.*

/** What AI adds to the API: the AI mutations, and the service behind them. */
object AiModule {

  type Env = AiService

  def apis: List[GraphQL[Env]] = List(AiApi.api)

  val layers: RLayer[AppConfig, Env] = AppConfig.ai >>> AiService.live

}
