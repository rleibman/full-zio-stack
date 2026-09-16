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

package net.leibman.fullziostack.telemetry

import caliban.CalibanError
import caliban.wrappers.Wrapper.OverallWrapper
import io.opentelemetry.api
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import zio.*
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.tracing.Tracing

case class TelemetryConfig(
  serviceName: String,
  /** OTLP/HTTP endpoint of a collector, e.g. `http://localhost:4318`. Empty: tracing is a no-op. */
  endpoint: Option[String] = None
)

/** OpenTelemetry tracing, through zio-telemetry. Spans are exported only when an endpoint is configured; otherwise
  * everything below still runs, against a tracer that records nothing.
  */
object Telemetry {

  val live: RLayer[TelemetryConfig, Tracing] =
    ZLayer.service[TelemetryConfig].flatMap { config =>
      val openTelemetry: TaskLayer[api.OpenTelemetry] = config.get.endpoint.filter(_.nonEmpty) match {
        case None           => OpenTelemetry.noop
        case Some(endpoint) => OpenTelemetry.custom(sdk(config.get.serviceName, endpoint))
      }
      openTelemetry ++ OpenTelemetry.contextZIO >>> OpenTelemetry.tracing(config.get.serviceName)
    }

  private def sdk(
    serviceName: String,
    endpoint:    String
  ): ZIO[Scope, Throwable, api.OpenTelemetry] =
    ZIO.fromAutoCloseable(ZIO.attempt {
      val exporter: OtlpHttpSpanExporter =
        OtlpHttpSpanExporter.builder().nn.setEndpoint(s"${endpoint.stripSuffix("/")}/v1/traces").nn.build().nn
      val attributes: Attributes = Attributes.of(AttributeKey.stringKey("service.name"), serviceName).nn
      val resource: Resource = Resource.getDefault.nn.merge(Resource.create(attributes)).nn
      val tracerProvider: SdkTracerProvider =
        SdkTracerProvider
          .builder().nn
          .addSpanProcessor(BatchSpanProcessor.builder(exporter).nn.build()).nn
          .setResource(resource).nn
          .build().nn
      OpenTelemetrySdk.builder().nn.setTracerProvider(tracerProvider).nn.build().nn
    })

  /** A span around each GraphQL operation, named after it. */
  val graphqlSpans: OverallWrapper[Tracing] = new OverallWrapper[Tracing] {

    override def wrap[R <: Tracing](
      process: caliban.GraphQLRequest => ZIO[R, Nothing, caliban.GraphQLResponse[CalibanError]]
    ): caliban.GraphQLRequest => ZIO[R, Nothing, caliban.GraphQLResponse[CalibanError]] =
      request =>
        ZIO.serviceWithZIO[Tracing](tracing =>
          tracing.span(s"graphql ${request.operationName.getOrElse("operation")}")(process(request))
        )

  }

}
