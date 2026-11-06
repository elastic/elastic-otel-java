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
package co.elastic.otel.spanname;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpanNamingSpanExporterTest {

  private InMemorySpanExporter memoryExporter;
  private SpanNameSettings settings;
  private SdkTracerProvider tracerProvider;
  private Tracer tracer;

  @BeforeEach
  void setUp() {
    memoryExporter = InMemorySpanExporter.create();
    settings = new SpanNameSettings();
    tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                SimpleSpanProcessor.create(new SpanNamingSpanExporter(memoryExporter, settings)))
            .build();
    tracer = tracerProvider.get("test");
  }

  @AfterEach
  void tearDown() {
    tracerProvider.close();
  }

  @Test
  void disabledByDefault() {
    init(new HashMap<>());

    serverSpan("GET", "/users/42/cart");

    assertThat(exportedNames()).containsExactly("GET");
  }

  @Test
  void usePathAsSpanName() {
    init(usePath());

    serverSpan("GET", "/users/42/cart");
    serverSpan("HTTP", "/users/42/cart");

    assertThat(exportedNames()).containsExactly("GET /users/42/cart", "HTTP /users/42/cart");
  }

  @Test
  void usePathAsSpanName_keepsRouteName() {
    init(usePath());

    tracer
        .spanBuilder("GET /users/{id}/cart")
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(HTTP_REQUEST_METHOD, "GET")
        .setAttribute(URL_PATH, "/users/42/cart")
        .startSpan()
        .end();

    assertThat(exportedNames()).containsExactly("GET /users/{id}/cart");
  }

  @Test
  void usePathAsSpanName_withoutPath() {
    init(usePath());

    tracer
        .spanBuilder("GET")
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(HTTP_REQUEST_METHOD, "GET")
        .startSpan()
        .end();

    assertThat(exportedNames()).containsExactly("GET");
  }

  @Test
  void usePathAsSpanName_onlyServerSpans() {
    init(usePath());

    for (SpanKind kind : new SpanKind[] {SpanKind.CLIENT, SpanKind.INTERNAL}) {
      tracer
          .spanBuilder("GET")
          .setSpanKind(kind)
          .setAttribute(HTTP_REQUEST_METHOD, "GET")
          .setAttribute(URL_PATH, "/users/42/cart")
          .startSpan()
          .end();
    }

    assertThat(exportedNames()).containsExactly("GET", "GET");
  }

  @Test
  void spanNameGroups() {
    Map<String, String> config = usePath();
    config.put(SpanNameSettings.SPAN_NAME_GROUPS, "GET /users/*/cart, GET /orders/*");
    init(config);

    serverSpan("GET", "/users/42/cart");
    serverSpan("GET", "/users/73/cart");
    serverSpan("GET", "/orders/1");
    serverSpan("GET", "/users/42");
    serverSpan("POST", "/users/42/cart");

    assertThat(exportedNames())
        .containsExactly(
            "GET /users/*/cart",
            "GET /users/*/cart",
            "GET /orders/*",
            "GET /users/42",
            "POST /users/42/cart");
  }

  @Test
  void spanNameGroups_caseSensitivity() {
    Map<String, String> config = usePath();
    config.put(SpanNameSettings.SPAN_NAME_GROUPS, "GET /Users/*");
    init(config);

    serverSpan("GET", "/Users/42");
    serverSpan("GET", "/users/42");

    assertThat(exportedNames()).containsExactly("GET /Users/*", "GET /users/42");
  }

  @Test
  void spanNameGroups_withoutUsePath() {
    Map<String, String> config = new HashMap<>();
    config.put(SpanNameSettings.SPAN_NAME_GROUPS, "GET /users/*,process orders-*");
    init(config);

    tracer.spanBuilder("GET /users/{id}").startSpan().end();
    tracer.spanBuilder("process orders-123").startSpan().end();
    // path is not used to build the name, so the group does not apply
    serverSpan("GET", "/users/42");

    assertThat(exportedNames()).containsExactly("GET /users/*", "process orders-*", "GET");
  }

  @Test
  void spanNameGroups_allSpans() {
    Map<String, String> config = new HashMap<>();
    config.put(SpanNameSettings.SPAN_NAME_GROUPS, "child *");
    init(config);

    Span root = tracer.spanBuilder("child root").startSpan();
    try (Scope ignored = root.makeCurrent()) {
      tracer.spanBuilder("child 1").startSpan().end();
    } finally {
      root.end();
    }

    assertThat(exportedNames()).containsExactly("child *", "child *");
  }

  @Test
  void runtimeUpdates() {
    Map<String, String> config = usePath();
    config.put(SpanNameSettings.SPAN_NAME_GROUPS, "GET /users/*");
    init(config);

    settings.setUsePathAsSpanName(false);
    settings.setSpanNameGroups("GET /orders/*");
    serverSpan("GET", "/users/42");
    tracer.spanBuilder("GET /orders/{id}").startSpan().end();
    assertExportedNames("GET", "GET /orders/*");

    settings.setUsePathAsSpanName(true);
    settings.setSpanNameGroups("");
    serverSpan("GET", "/users/42");
    assertExportedNames("GET /users/42");
  }

  private void init(Map<String, String> config) {
    settings.init(DefaultConfigProperties.createFromMap(config));
  }

  private static Map<String, String> usePath() {
    Map<String, String> config = new HashMap<>();
    config.put(SpanNameSettings.USE_PATH_AS_SPAN_NAME, "true");
    return config;
  }

  private void serverSpan(String name, String path) {
    tracer
        .spanBuilder(name)
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(HTTP_REQUEST_METHOD, "HTTP".equals(name) ? "_OTHER" : name)
        .setAttribute(URL_PATH, path)
        .startSpan()
        .end();
  }

  private void assertExportedNames(String... names) {
    assertThat(exportedNames()).containsExactly(names);
    memoryExporter.reset();
  }

  private List<String> exportedNames() {
    return memoryExporter.getFinishedSpanItems().stream()
        .map(SpanData::getName)
        .collect(Collectors.toList());
  }
}
