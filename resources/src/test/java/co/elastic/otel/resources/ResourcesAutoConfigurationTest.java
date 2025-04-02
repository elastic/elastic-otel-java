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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.resources.ResourceProviderPropertiesCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourcesAutoConfigurationTest {

  private static final List<String> LIST = Arrays.asList(
      config("gcp"),
      config("aws"),
      config("azure"));

  private static String config(String provider) {
    return String.format("otel.resource.providers.%s.enabled", provider);
  }

  @Test
  void elastic_defaults() {
    // default everything should be enabled

    Map<String, String> explicitConfig = Collections.emptyMap();
    Map<String, String> expectedResult = new HashMap<>();

    LIST.forEach(v -> expectedResult.put(v, "true"));

    testConfig(explicitConfig, expectedResult);
  }

  @Test
  void explicitly_enabled() {
    Map<String, String> explicitConfig = new HashMap<>();
    explicitConfig.put(LIST.get(0), "true");

    Map<String, String> expectedResult = new HashMap<>();
    LIST.forEach(v -> expectedResult.put(v, "true"));

    testConfig(explicitConfig, expectedResult);
  }

  @Test
  void explicitly_disabled() {
    Map<String, String> explicitConfig = new HashMap<>();
    explicitConfig.put(LIST.get(0), "false");

    Map<String, String> expectedResult = new HashMap<>();
    expectedResult.put(LIST.get(0), "false");
    for (int i = 1; i < LIST.size(); i++) {
      expectedResult.put(LIST.get(i), "true");
    }

    testConfig(explicitConfig, expectedResult);
  }

  private static void testConfig(
      Map<String, String> explicitConfig, Map<String, String> expectedResult) {
    DefaultConfigProperties empty = DefaultConfigProperties.createFromMap(explicitConfig);
    Map<String, String> result = ResourcesAutoConfiguration.customize(empty);
    assertThat(result).containsAllEntriesOf(expectedResult);
  }
}
