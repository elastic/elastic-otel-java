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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.HashMap;
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

  private static void testEndpoint(
      String configValue, String expectedEndpoint, String description) {
    Map<String, String> map = Collections.emptyMap();
    if (configValue != null) {
      map = Collections.singletonMap("elastic.otel.opamp.endpoint", configValue);
    }
    assertThat(CentralConfig.getEndpoint(DefaultConfigProperties.createFromMap(map)))
        .describedAs(description)
        .isEqualTo(expectedEndpoint);
  }

  @Test
  void getServiceName() {
    Map<String, String> map = Collections.emptyMap();
    testServiceName(map, "unknown_service:java", "default service name should be provided");

    map = Collections.singletonMap("otel.service.name", "my-service-1");
    testServiceName(map, "my-service-1", "set through service name config");

    map = Collections.singletonMap("otel.resource.attributes", "service.name=my-service-2");
    testServiceName(map, "my-service-2", "set through resource attributes config");

    map = new HashMap<>();
    map.put("otel.service.name", "my-service-3");
    map.put("otel.resource.attributes", "service.name=my-service-4");
    testServiceName(map, "my-service-3", "service name takes precedence over resource attributes");

    map.clear();
    map.put("otel.resource.attributes", "");
    testServiceName(map, "unknown_service:java", "default service name should be provided");

    map.clear();
    map.put("otel.resource.attributes", "service.name=");
    testServiceName(map, "unknown_service:java", "default service name should be provided");
  }

  private static void testServiceName(
      Map<String, String> map, String expectedServiceName, String description) {
    ConfigProperties configProperties = DefaultConfigProperties.createFromMap(map);
    assertThat(CentralConfig.getServiceName(configProperties))
        .isNotNull()
        .describedAs(description)
        .isEqualTo(expectedServiceName);
  }

  @Test
  void getServiceEnvironment() {
    Map<String, String> map = Collections.emptyMap();
    testServiceEnvironment(map, null, "no environment by default");

    map = Collections.singletonMap("otel.resource.attributes", "deployment.environment.name=test1");
    testServiceEnvironment(map, "test1", "environment set through resource attribute");

    map = Collections.singletonMap("otel.resource.attributes", "deployment.environment=test2");
    testServiceEnvironment(map, "test2", "environment set through legacy resource attribute");

    map =
        Collections.singletonMap(
            "otel.resource.attributes",
            "deployment.environment=test3,deployment.environment.name=test4");
    testServiceEnvironment(map, "test4", "when both set semconv attribute takes precedence");
  }

  private static void testServiceEnvironment(
      Map<String, String> map, String expectedEnvironment, String description) {
    ConfigProperties configProperties = DefaultConfigProperties.createFromMap(map);
    assertThat(CentralConfig.getServiceEnvironment(configProperties))
        .describedAs(description)
        .isEqualTo(expectedEnvironment);
  }
}
