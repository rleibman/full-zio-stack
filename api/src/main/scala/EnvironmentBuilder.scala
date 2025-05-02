import com.zaxxer.hikari.HikariDataSource
import config.ConfigurationService
import db.{MockDataServices, ModelObjectDataService, QuillDataServices}
import zio.*
import zio.logging.backend.SLF4J

object EnvironmentBuilder {

  val mock: ULayer[ConfigurationService & ModelObjectDataService] = ZLayer.make[ConfigurationService & ModelObjectDataService](
    ConfigurationService.live,
    MockDataServices.modelObjectDataServices
  )

  private val dataServiceLayer: URLayer[ConfigurationService, HikariDataSource] = ZLayer.fromZIO {
    for {
      config <- ZIO.serviceWithZIO[ConfigurationService](_.appConfig)
    } yield config.dataSource
  }.orDie

  val live: ULayer[ConfigurationService & ModelObjectDataService] = ZLayer.make[ConfigurationService & ModelObjectDataService](
    ConfigurationService.live,
    QuillDataServices.live,
    dataServiceLayer
  )

}
