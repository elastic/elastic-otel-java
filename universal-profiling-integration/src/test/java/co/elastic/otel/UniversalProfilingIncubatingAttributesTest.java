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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes;
import org.junit.jupiter.api.Test;

public class UniversalProfilingIncubatingAttributesTest {

  @Test
  void serviceNamespace() {
    check(
        UniversalProfilingIncubatingAttributes.SERVICE_NAMESPACE,
        ServiceIncubatingAttributes.SERVICE_NAMESPACE);
  }

  @Test
  void hostId() {
    check(UniversalProfilingIncubatingAttributes.HOST_ID, HostIncubatingAttributes.HOST_ID);
  }

  private static <T> void check(AttributeKey<T> attribute, AttributeKey<?> incubatingAttribute) {
    assertThat(attribute)
        .describedAs(
            "incubating attribute %s has changed and is no more equal to %s",
            attribute, incubatingAttribute)
        .isEqualTo(incubatingAttribute);
  }
}
