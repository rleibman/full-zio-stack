package apiClient

import zio.*
import zio.http.endpoint.{EndpointExecutor, EndpointLocator}
import zio.http.{Client, url}

trait HelloClient[F[_]] {

  def greeting: F[HelloEndpoints.ResponseBody]

}

case class ZioHelloClient(executor: EndpointExecutor[Any, Unit, Scope]) extends HelloClient[UIO] {

  def greeting: UIO[HelloEndpoints.ResponseBody] = ZIO.scoped(executor(HelloEndpoints.getGreeting(())))

}

object ZioHelloClient {

  def live: URLayer[EndpointLocator & Client, ZioHelloClient] =
    ZLayer.fromZIO {
      for {
        client  <- ZIO.service[Client]
        locator <- ZIO.service[EndpointLocator]
      } yield ZioHelloClient(EndpointExecutor(client, locator))
    }

}

object TestHello extends ZIOApp {

  override type Environment = Client & EndpointLocator & ZioHelloClient

  override def environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]

  override def bootstrap: TaskLayer[Environment] =
    ZLayer.make[Environment](
      Client.default,
      ZLayer.succeed(EndpointLocator.fromURL(url"http://localhost:8080")),
      ZioHelloClient.live
    )

  override def run = ZIO.serviceWith[ZioHelloClient](_.greeting)

}
