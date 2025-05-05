package apiClient

import zio.*
import zio.cli.HelpDoc
import zio.http.endpoint.cli.HttpCliApp
import zio.http.endpoint.{EndpointExecutor, EndpointLocator}
import zio.http.{Client, url}

object TestHello extends ZIOAppDefault {

  override def run: Task[Hello.GET.ResponseBody] = {
    (for {
      client <- ZIO.service[Client]
      executor = EndpointExecutor(client, EndpointLocator.fromURL(url"http://localhost:8080"))
      response <- ZIO.scoped(executor(Hello.getGreeting(())))
      _        <- ZIO.logInfo(s"Response: ${response.message}")
    } yield response).provide(
      Client.default
    )
  }

  def test = {
    val app = HttpCliApp.fromEndpoints(
      name = "endpoints",
      version = "0.0.1",
      summary = HelpDoc.Span.text(""),
      host = "localhost",
      port = 1234,
      endpoints = Chunk(Hello.getGreeting)
    )
    app
  }

}
