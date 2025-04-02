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
package co.elastic.otel.resources;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.Map;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ResourcesAutoConfiguration implements AutoConfigurationCustomizerProvider {

  private static final String[] PROVIDERS = {"aws", "gcp", "azure"};

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesCustomizer(ResourcesAutoConfiguration::customize);
  }

  @Override
  public int order() {
    // execute between the last user-provided provider and
    // io.opentelemetry.instrumentation.resources.ResourceProviderPropertiesCustomizer
    return Integer.MAX_VALUE - 1;
  }

  // visible for testing
  static Map<String, String> customize(ConfigProperties config) {

    Map<String, String> result = new HashMap<>();
    for (String provider : PROVIDERS) {
      String key = String.format("otel.resource.providers.%s.enabled", provider);
      boolean value = config.getBoolean(key, true);
      result.put(key, Boolean.toString(value));
    }

    return result;
  }
}
