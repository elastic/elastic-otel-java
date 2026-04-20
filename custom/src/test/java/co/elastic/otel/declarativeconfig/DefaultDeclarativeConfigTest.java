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
package co.elastic.otel.declarativeconfig;

import static co.elastic.otel.declarativeconfig.ElasticDeclarativeConfigurationCustomizerTest.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.javaagent.tooling.resources.ResourceCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.InputStream;
import java.util.function.Consumer;
import net.javacrumbs.jsonunit.assertj.JsonListAssert;
import org.junit.jupiter.api.Test;

public class DefaultDeclarativeConfigTest {

  // For now, we can't override env variable for testing, thus we just verify the default values
  // in the configuration we provide.
  @Test
  void testDefaults() {
    test(
        (config) -> {
          assertThat(config.getResource()).isNotNull();
          assertThat(config.getResource().getAttributesList()).isNull();

          assertThatJson(json(config.getResource())).inPath("attributes").isArray().isEmpty();
          JsonListAssert detectorsAssert =
              assertThatJson(json(config.getResource()))
                  .inPath("detection/development.detectors")
                  .isArray()
                  .hasSize(9);

          // those are the providers magically added by upstream and elastic distributions
          detectorsAssert
              .first()
              .isEqualTo(json("{\"opentelemetry_javaagent_distribution\":null}"));
          detectorsAssert.last().isEqualTo(json("{\"elastic_distribution\":null}"));

          // those are the ones that should be included in the default configuration
          detectorsAssert.contains(
              json("{\"aws\":null}"),
              json("{\"gcp\":null}"),
              json("{\"azure\":null}"),
              // TODO: maybe investigate why those are including empty objects
              json("{\"process\":{}}}"),
              json("{\"container\":{}}}"),
              json("{\"service\":{}}}"),
              json("{\"host\":{}}}"));

          assertThat(config.getTracerProvider()).isNotNull();
          assertThat(assertThat(config.getTracerProvider().getProcessors()).hasSize(1));
          assertThatJson(json((config.getTracerProvider().getProcessors().get(0))))
              .inPath("batch.exporter.otlp_http")
              .isObject()
              .containsEntry("endpoint", "http://localhost:4318/v1/traces");

          assertThat(config.getMeterProvider()).isNotNull();
          assertThat(assertThat(config.getMeterProvider().getReaders()).hasSize(1));
          assertThatJson(json((config.getMeterProvider().getReaders().get(0))))
              .inPath("periodic.exporter.otlp_http")
              .isObject()
              .containsEntry("endpoint", "http://localhost:4318/v1/metrics");

          assertThat(config.getLoggerProvider()).isNotNull();
          assertThat(assertThat(config.getLoggerProvider().getProcessors()).hasSize(1));
          assertThatJson(json((config.getLoggerProvider().getProcessors().get(0))))
              .inPath("batch.exporter.otlp_http")
              .isObject()
              .containsEntry("endpoint", "http://localhost:4318/v1/logs");

          assertThatJson(json(config.getInstrumentationDevelopment()))
              .describedAs("experimental jvm runtime telemetry metrics enabled by default")
              .inPath("java.runtime_telemetry.emit_experimental_metrics/development")
              .isBoolean()
              .isTrue();
        });
  }

  private static void test(Consumer<OpenTelemetryConfigurationModel> configChecks) {

    InputStream input =
        DefaultDeclarativeConfigTest.class
            .getClassLoader()
            .getResourceAsStream("co/elastic/otel/config.yaml");
    assertThat(input).isNotNull();
    OpenTelemetryConfigurationModel config = DeclarativeConfiguration.parse(input);

    // manually apply config customization for simplicity
    config = applyConfigCustomize(config, new ElasticDeclarativeConfigurationCustomizer());
    config = applyConfigCustomize(config, new ResourceCustomizerProvider());

    configChecks.accept(config);
  }
}
