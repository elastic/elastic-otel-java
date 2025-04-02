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

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourcesAutoConfigurationTest {

  public static final String GCP_ENABLED = "otel.resource.providers.gcp.enabled";
  public static final String AWS_ENABLED = "otel.resource.providers.aws.enabled";
  public static final String AZURE_ENABLED = "otel.resource.providers.azure.enabled";

  @Test
  void elastic_defaults() {
    // default everything should be enabled

    Map<String, String> explicitConfig = Collections.emptyMap();
    Map<String, String> expectedResult = new HashMap<>();
    expectedResult.put(GCP_ENABLED, "true");
    expectedResult.put(AWS_ENABLED, "true");
    expectedResult.put(AZURE_ENABLED, "true");

    testConfig(explicitConfig, expectedResult);
  }

  @Test
  void explicitly_enabled() {
    Map<String, String> explicitConfig = new HashMap<>();
    explicitConfig.put(GCP_ENABLED, "true");

    Map<String, String> expectedResult = new HashMap<>();
    expectedResult.put(GCP_ENABLED, "true");
    expectedResult.put(AWS_ENABLED, "true");
    expectedResult.put(AZURE_ENABLED, "true");

    testConfig(explicitConfig, expectedResult);
  }

  @Test
  void explicitly_disabled() {
    Map<String, String> explicitConfig = new HashMap<>();
    explicitConfig.put(GCP_ENABLED, "false");

    Map<String, String> expectedResult = new HashMap<>();
    expectedResult.put(GCP_ENABLED, "false");
    expectedResult.put(AWS_ENABLED, "true");
    expectedResult.put(AZURE_ENABLED, "true");

    testConfig(explicitConfig, expectedResult);
  }

  private static void testConfig(
      Map<String, String> explicitConfig, Map<String, String> expectedResult) {
    DefaultConfigProperties empty = DefaultConfigProperties.createFromMap(explicitConfig);
    Map<String, String> result = ResourcesAutoConfiguration.customize(empty);
    assertThat(result).containsAllEntriesOf(expectedResult);
  }
}
