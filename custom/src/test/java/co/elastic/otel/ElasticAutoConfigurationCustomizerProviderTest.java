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
package co.elastic.otel;

import static co.elastic.otel.ElasticAutoConfigurationCustomizerProvider.propertiesCustomizer;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.javaagent.tooling.EmptyConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ElasticAutoConfigurationCustomizerProviderTest {

  @Test
  void defaultConfiguration() {
    Map<String, String> config = propertiesCustomizer(EmptyConfigProperties.INSTANCE);
    assertThat(config)
        .describedAs("upstream distro version resource provider must be disabled")
        .containsEntry(
            "otel.java.disabled.resource.providers",
            "io.opentelemetry.javaagent.tooling.DistroVersionResourceProvider");

    assertThat(config)
        .describedAs("runtime experimental metrics must be enabled")
        .containsEntry(
            "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry", "true");
  }

  @Test
  void disableCustomResourceProvider() {
    Map<String, String> userConfig = new HashMap<>();
    userConfig.put("otel.java.disabled.resource.providers", "my.disabled.provider.Provider");
    Map<String, String> config = propertiesCustomizer(DefaultConfigProperties.create(userConfig));
    String value = config.get("otel.java.disabled.resource.providers");
    assertThat(value)
        .satisfies(
            v ->
                assertThat(v.split(","))
                    .containsExactly(
                        "io.opentelemetry.javaagent.tooling.DistroVersionResourceProvider",
                        "my.disabled.provider.Provider"));
  }

  @Test
  void disableExperimentalRuntimeMetrics() {
    Map<String, String> userConfig = new HashMap<>();
    userConfig.put("otel.instrumentation.runtime-telemetry.emit-experimental-telemetry", "false");
    Map<String, String> config = propertiesCustomizer(DefaultConfigProperties.create(userConfig));
    String value = config.get("otel.instrumentation.runtime-telemetry.emit-experimental-telemetry");
    assertThat(value).isEqualTo("false");
  }

  @Test
  void ensureDefaultMetricTemporalityIsDelta() {
    Map<String, String> config =
        propertiesCustomizer(DefaultConfigProperties.create(new HashMap<>()));
    String value = config.get("otel.exporter.otlp.metrics.temporality.preference");
    assertThat(value).isEqualTo("DELTA");
  }

  @Test
  void customizeMetricTemporalityPreference() {
    Map<String, String> userConfig = new HashMap<>();
    userConfig.put("otel.exporter.otlp.metrics.temporality.preference", "LOWMEMORY");
    Map<String, String> config = propertiesCustomizer(DefaultConfigProperties.create(userConfig));
    String value = config.get("otel.exporter.otlp.metrics.temporality.preference");
    assertThat(value).isEqualTo("LOWMEMORY");
  }
}
