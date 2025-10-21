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
package co.elastic.otel.dynamicconfig;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CentralConfigTest {

  @Test
  void getEndpoint() {
    testEndpoint(null, null, "missing config should return null");
    testEndpoint("", null, "empty config should return null");
    testEndpoint(
        "http://localhost:8080/v1/opamp",
        "http://localhost:8080/v1/opamp",
        "opamp suffix should be automatically added");
    testEndpoint(
        "http://localhost:8080/",
        "http://localhost:8080/v1/opamp",
        "opamp suffix should be automatically added");
    testEndpoint(
        "http://localhost:8080",
        "http://localhost:8080/v1/opamp",
        "opamp suffix should be automatically added");
  }

  private void testEndpoint(String configValue, String expectedEndpoint, String description) {
    Map<String, String> map = Collections.emptyMap();
    if (configValue != null) {
      map = Collections.singletonMap("elastic.otel.opamp.endpoint", configValue);
    }
    assertThat(CentralConfig.getEndpoint(DefaultConfigProperties.createFromMap(map)))
        .describedAs(description)
        .isEqualTo(expectedEndpoint);
  }
}
