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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ElasticAutoConfigurationCustomizerProvider
    implements AutoConfigurationCustomizerProvider {

  private static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
  private static final String RUNTIME_EXPERIMENTAL_TELEMETRY =
      "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {

    autoConfiguration
        .addTracerProviderCustomizer(
            (sdkTracerProviderBuilder, configProperties) ->
                // span processor registration
                sdkTracerProviderBuilder.addSpanProcessor(
                    ElasticExtension.INSTANCE.getSpanProcessor()))
        .addPropertiesCustomizer(ElasticAutoConfigurationCustomizerProvider::propertiesCustomizer)
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) ->
                // wrap the original span exporter
                ElasticExtension.INSTANCE.wrapSpanExporter(spanExporter))
        .addMetricExporterCustomizer((exporter, config) -> new ElasticMetricExporter(exporter));
  }

  static Map<String, String> propertiesCustomizer(ConfigProperties configProperties) {
    Set<String> disabledResourceProviders =
        new HashSet<>(configProperties.getList(DISABLED_RESOURCE_PROVIDERS));

    // disable upstream distro name & version provider
    disabledResourceProviders.add(
        "io.opentelemetry.javaagent.tooling.DistroVersionResourceProvider");

    Map<String, String> config = new HashMap<>();

    // enable experimental telemetry metrics by default if not explicitly disabled
    boolean experimentalTelemetry =
        configProperties.getBoolean(RUNTIME_EXPERIMENTAL_TELEMETRY, true);
    config.put(RUNTIME_EXPERIMENTAL_TELEMETRY, Boolean.toString(experimentalTelemetry));

    config.put(DISABLED_RESOURCE_PROVIDERS, String.join(",", disabledResourceProviders));

    return config;
  }
}
