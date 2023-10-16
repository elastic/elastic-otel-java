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

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.Collections;

public class ElasticAutoConfigurationCustomizerprovider
    implements AutoConfigurationCustomizerProvider {

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
              // Wrap context storage when configuration is loaded,
              // configuration customization is used as an init hook but does not actually alter it.
              ContextStorage.addWrapper(ElasticExtension.INSTANCE::wrapContextStorage);
              return Collections.emptyMap();
            })
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) ->
                // wrap the original span exporter
                ElasticExtension.INSTANCE.wrapSpanExporter(spanExporter))
        .addMetricExporterCustomizer((exporter, config) -> new ElasticMetricExporter(exporter));
  }
}
