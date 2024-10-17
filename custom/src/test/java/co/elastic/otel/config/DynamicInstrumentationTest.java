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
package co.elastic.otel.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.internal.ScopeConfigurator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

public class DynamicInstrumentationTest {
  @Test
  // Functional testing is in DynamicInstrumentationSmokeTest
  // These tests are so that when the SDK implementation stops being
  // experimental, we switch from reflection to actual method calls
  public void checkForPublicImplementations() throws NoSuchMethodException, ClassNotFoundException {

    Method method1 =
        SdkTracerProviderBuilder.class.getDeclaredMethod(
            "setTracerConfigurator", ScopeConfigurator.class);
    assertThat(Modifier.toString(method1.getModifiers())).isNotEqualTo("public");

    Method method2 =
        SdkTracerProvider.class.getDeclaredMethod(
            "getTracerConfig", InstrumentationScopeInfo.class);
    assertThat(Modifier.toString(method2.getModifiers())).isNotEqualTo("public");

    Class<?> sdkTracer = Class.forName("io.opentelemetry.sdk.trace.SdkTracer");
    Method method3 = sdkTracer.getDeclaredMethod("getInstrumentationScopeInfo");
    assertThat(Modifier.toString(method3.getModifiers())).isNotEqualTo("public");

    assertThatThrownBy(
            () -> SdkTracerProvider.class.getDeclaredMethod("updateTracerConfigurations"))
        .isInstanceOf(NoSuchMethodException.class);
  }
}
