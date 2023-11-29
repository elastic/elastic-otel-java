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

import co.elastic.otel.config.Configurations;
import co.elastic.otel.resources.ElasticResourceProvider;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

public class ElasticAutoConfigurationCustomizerprovider
    implements AutoConfigurationCustomizerProvider {

  public static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {

    autoConfiguration
        .addResourceCustomizer(
            (resource, configProperties) -> {
              // create the resource provider ourselves we can store a reference to it
              // and that will only get "fast" resources attributes when invoked
              ElasticResourceProvider resourceProvider = new ElasticResourceProvider(false);
              ElasticExtension.INSTANCE.registerResourceProvider(resourceProvider);
              return resource.merge(resourceProvider.createResource(configProperties));
            })
        .addTracerProviderCustomizer(
            (sdkTracerProviderBuilder, configProperties) ->
                // span processor registration
                sdkTracerProviderBuilder.addSpanProcessor(
                    ElasticExtension.INSTANCE.getSpanProcessor()))
        .addPropertiesCustomizer(
            configProperties -> {
              // Wrap context storage when configuration is loaded,
              // configuration customization is used as an init hook but does not actually alter it.
              ContextStorage.addWrapper(ElasticExtension.INSTANCE::wrapContextStorage);
              return Collections.emptyMap();
            })
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) ->
                // wrap the original span exporter
                ElasticExtension.INSTANCE.wrapSpanExporter(spanExporter))
        .addMetricExporterCustomizer((exporter, config) -> new ElasticMetricExporter(exporter))
        .addPropertiesSupplier(this::getDefaultProperties)
        .addPropertiesCustomizer(getPropertiesCustomizer());
  }

  private Map<String, String> getDefaultProperties() {
    Map<String, String> properties = new HashMap<>();
    Configurations configurations = Configurations.getInstance();
    if (configurations.getSecretToken() != null) {
      properties.put(
          "otel.exporter.otlp.headers", "Authorization=Bearer " + configurations.getSecretToken());
    }
    properties.put(
        "otel.javaagent.debug", Boolean.toString(configurations.getLogLevel().isDebug()));
    //    switchDebugging(configurations.getLogLevel().isDebug(), properties);
    return properties;
  }

  private Function<ConfigProperties, Map<String, String>> getPropertiesCustomizer() {
    return new Function<ConfigProperties, Map<String, String>>() {
      @Override
      public Map<String, String> apply(ConfigProperties configProperties) {
        Map<String, String> properties = new HashMap<>();
        Configurations configurations = Configurations.getInstance();
        String elasticEndpoint = configurations.getServerUrl().toString();
        // The three otel.exporter.otlp.*.endpoint and fallback otel.exporter.otlp.endpoint have a
        // relationship
        // It's not possible to reset the defaults and keep the relationship consistent, so we set
        // them here
        // If nothing was set, the values for the three here are null, with fallback
        // http://backend:8080, so:
        // 1. If the fallback is set to anything other than http://backend:8080, we don't change
        // anything
        // 2. If the fallback is set to http://backend:8080, we change it and any of the three that
        // are null, to elasticEndpoint
        String fallbackEndpoint = configProperties.getString("otel.exporter.otlp.endpoint");
        if ("http://backend:8080".equals(fallbackEndpoint)) {
          String tracesEndpoint = configProperties.getString("otel.exporter.otlp.traces.endpoint");
          if (tracesEndpoint == null) {
            properties.put("otel.exporter.otlp.traces.endpoint", elasticEndpoint);
          }
          String metricsEndpoint =
              configProperties.getString("otel.exporter.otlp.metrics.endpoint");
          if (metricsEndpoint == null) {
            properties.put("otel.exporter.otlp.metrics.endpoint", elasticEndpoint);
          }
          String logsEndpoint = configProperties.getString("otel.exporter.otlp.logs.endpoint");
          if (logsEndpoint == null) {
            properties.put("otel.exporter.otlp.logs.endpoint", elasticEndpoint);
          }
        }
        // logAllProperties(configProperties);
        return properties;
      }
    };
  }

  // Kept for convenience for now
  private void logAllProperties(ConfigProperties configProperties) {
    if (configProperties instanceof DefaultConfigProperties) {
      try {
        Field configField = DefaultConfigProperties.class.getDeclaredField("config");
        configField.setAccessible(true);
        Map<String, String> config = (Map<String, String>) configField.get(configProperties);
        for (String key : new TreeSet<>(config.keySet())) {
          String value = config.get(key);
          System.out.println(key + " -> " + value);
        }
      } catch (Exception e) {
        // ignore, we just don't log anything
      }
    }
  }
}
