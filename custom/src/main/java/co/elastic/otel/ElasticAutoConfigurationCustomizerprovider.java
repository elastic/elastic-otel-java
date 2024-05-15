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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ElasticAutoConfigurationCustomizerprovider
    implements AutoConfigurationCustomizerProvider {

  private static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {

    autoConfiguration
        .addTracerProviderCustomizer(
            (sdkTracerProviderBuilder, configProperties) ->
                // span processor registration
                sdkTracerProviderBuilder.addSpanProcessor(
                    ElasticExtension.INSTANCE.getSpanProcessor()))
        .addPropertiesCustomizer(
            configProperties -> {
              Set<String> disabledConfig =
                  new HashSet<>(configProperties.getList(DISABLED_RESOURCE_PROVIDERS));

              // disabling embedded resource providers

              // disable upstream distro name & version provider
              disabledConfig.add(
                  "io.opentelemetry.javaagent.tooling.DistroVersionResourceProvider");

              Map<String, String> config = new HashMap<>();
              config.put(DISABLED_RESOURCE_PROVIDERS, String.join(",", disabledConfig));

              return config;
            })
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) ->
                // wrap the original span exporter
                ElasticExtension.INSTANCE.wrapSpanExporter(spanExporter))
        .addMetricExporterCustomizer((exporter, config) -> new ElasticMetricExporter(exporter));
  }
}
