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

import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.internal.ScopeConfigurator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.internal.TracerConfig;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Notes: 1. The instrumentation can't have been disabled by configuration, eg using
 * -Dotel.instrumentation.[name].enabled=false as in that case it is never initialized so can't be
 * "re-enabled" 2. The disable/re-enable is eventually consistent, needing the application to pass a
 * synchronization barrier to take effect - but for most applications these are very frequent
 */
public class DynamicInstrumentation {

  private static Object getField(String fieldname, Object target) {
    try {
      Field field = target.getClass().getDeclaredField(fieldname);
      field.setAccessible(true);
      return field.get(target);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Error getting " + fieldname + " from " + target.getClass(), e);
    }
  }

  private static <T> Object call(
      String methodname, Object target, T arg1, Class<? super T> arg1Class) {
    try {
      Method method = target.getClass().getDeclaredMethod(methodname, arg1Class);
      method.setAccessible(true);
      return method.invoke(target, arg1);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Error calling " + methodname + " on " + target.getClass() + "(" + arg1Class + ")", e);
    }
  }

  // SdkTracerProviderBuilder.setTracerConfigurator(ScopeConfigurator<<TracerConfig>> configurator)
  // here because it's not currently public
  public static SdkTracerProviderBuilder setTracerConfigurator(
      SdkTracerProviderBuilder sdkTracerProviderBuilder,
      ScopeConfigurator<TracerConfig> configurator) {
    call("setTracerConfigurator", sdkTracerProviderBuilder, configurator, ScopeConfigurator.class);
    return sdkTracerProviderBuilder;
  }

  // SdkTracerProvider.setTracerConfigurator(ScopeConfigurator tracerConfigurator)
  // here because it's not currently public
  public static TracerConfig setProviderTracerConfigurator(
      TracerProvider provider, ScopeConfigurator<TracerConfig> tracerConfigurator) {
    if (!(provider instanceof SdkTracerProvider)) {
      provider = (TracerProvider) getField("delegate", provider);
    }
    if (provider instanceof SdkTracerProvider) {
      return (TracerConfig)
          call("setTracerConfigurator", provider, tracerConfigurator, ScopeConfigurator.class);
    } else {
      throw new IllegalStateException(
          "Expected SdkTracerProvider but got " + provider.getClass().getName());
    }
  }

  static {
    // will refactor this when DynamicInstrumentation class becomes mostly empty
    DynamicConfigurationPropertyChecker.startCheckerThread();
  }
}
