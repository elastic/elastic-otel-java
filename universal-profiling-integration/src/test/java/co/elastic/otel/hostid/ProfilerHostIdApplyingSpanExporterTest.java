/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.hostid;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProfilerHostIdApplyingSpanExporterTest {

  @BeforeEach
  @AfterEach
  public void resetProfilerProvidedHostId() {
    ProfilerProvidedHostId.set(null);
  }

  @Test
  public void checkHostIdUpdated() {
    Resource originalResource = Resource.builder().put("custom", "foobar").build();
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    try (SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .setResource(originalResource)
            .addSpanProcessor(
                SimpleSpanProcessor.create(new ProfilerHostIdApplyingSpanExporter(exporter)))
            .build()) {

      Tracer tracer = tracerProvider.get("dummy");

      tracer.spanBuilder("s1").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(data -> assertThat(data.getResource()).isSameAs(originalResource));

      ProfilerProvidedHostId.set("hooray");

      exporter.reset();
      tracer.spanBuilder("s2").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(
              data ->
                  assertThat(data.getResource().getAttributes())
                      .containsEntry("custom", "foobar")
                      .containsEntry(ResourceAttributes.HOST_ID, "hooray"));

      Resource updated = exporter.getFinishedSpanItems().get(0).getResource();

      // ensure caching works
      exporter.reset();
      tracer.spanBuilder("s3").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(data -> assertThat(data.getResource()).isSameAs(updated));

      // ensure a second update works
      ProfilerProvidedHostId.set("changed!");
      exporter.reset();
      tracer.spanBuilder("s4").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(
              data ->
                  assertThat(data.getResource().getAttributes())
                      .containsEntry("custom", "foobar")
                      .containsEntry(ResourceAttributes.HOST_ID, "changed!"));

      // and finally a reset
      ProfilerProvidedHostId.set("");
      exporter.reset();
      tracer.spanBuilder("s5").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(data -> assertThat(data.getResource()).isSameAs(originalResource));
    }
  }

  @Test
  public void ensureApplicationProvidedHostIdTakesPrecedence() {
    Resource originalResource =
        Resource.builder()
            .put("custom", "foobar")
            .put(ResourceAttributes.HOST_ID, "app-provided")
            .build();
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    try (SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .setResource(originalResource)
            .addSpanProcessor(
                SimpleSpanProcessor.create(new ProfilerHostIdApplyingSpanExporter(exporter)))
            .build()) {

      Tracer tracer = tracerProvider.get("dummy");

      ProfilerProvidedHostId.set("profiler-provided");

      tracer.spanBuilder("s1").startSpan().end();
      assertThat(exporter.getFinishedSpanItems())
          .hasSize(1)
          .first()
          .satisfies(data -> assertThat(data.getResource()).isSameAs(originalResource));
    }
  }
}
