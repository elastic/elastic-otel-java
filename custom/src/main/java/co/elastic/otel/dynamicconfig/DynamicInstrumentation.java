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

import static co.elastic.otel.dynamicconfig.DynamicConfiguration.INSTRUMENTATION_NAME_PREPEND;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.internal.ComponentRegistry;
import io.opentelemetry.sdk.internal.ScopeConfigurator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.internal.TracerConfig;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notes: 1. The instrumentation can't have been disabled by configuration, eg using
 * -Dotel.instrumentation.[name].enabled=false as in that case it is never initialized so can't be
 * "re-enabled" 2. The specific instrumentation name is used, you can see these by setting this
 * class logging level to j.u.l.Level.CONFIG 3. The disable/re-enable is eventually consistent,
 * needing the application to pass a synchronization barrier to take effect - but for most
 * applications these are very frequent
 */
public class DynamicInstrumentation {

  private static final Logger logger = Logger.getLogger(DynamicInstrumentation.class.getName());

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

  private static Object call(String methodname, Object target) {
    try {
      Method method = target.getClass().getDeclaredMethod(methodname);
      method.setAccessible(true);
      return method.invoke(target);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Error calling " + methodname + " on " + target.getClass(), e);
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

  // SdkTracerProvider.getTracerConfig(InstrumentationScopeInfo instrumentationScopeInfo)
  // here because it's not currently public
  private static TracerConfig getTracerConfig(
      SdkTracerProvider provider, InstrumentationScopeInfo instrumentationScopeInfo) {
    return (TracerConfig)
        call("getTracerConfig", provider, instrumentationScopeInfo, InstrumentationScopeInfo.class);
  }

  // SdkTracer.getInstrumentationScopeInfo()
  // here because it's not currently public
  private static InstrumentationScopeInfo getInstrumentationScopeInfo(Tracer sdkTracer)
      throws NoSuchFieldException, IllegalAccessException {
    return (InstrumentationScopeInfo) call("getInstrumentationScopeInfo", sdkTracer);
  }

  // Not an existing method
  // SdkTracerProvider.updateTracerConfigurations()
  // updates all tracers with the current SdkTracerProvider.tracerConfigurator
  // Code implementation equivalent to
  // this.tracerSdkComponentRegistry
  //        .getComponents()
  //        .forEach(
  //            sdkTracer ->
  //                sdkTracer.updateTracerConfig(
  //                    getTracerConfig(sdkTracer.getInstrumentationScopeInfo())));
  // where SdkTracer.updateTracerConfig(TracerConfig tracerConfig) is equivalent to
  //  this.tracerEnabled = tracerConfig.isEnabled();
  static void updateTracerConfigurations(TracerProvider provider) {
    if (!(provider instanceof SdkTracerProvider)) {
      provider = (TracerProvider) getField("delegate", provider);
    }
    ComponentRegistry<Tracer> tracerSdkComponentRegistry =
        (ComponentRegistry<Tracer>) getField("tracerSdkComponentRegistry", provider);
    SdkTracerProvider finalProvider = (SdkTracerProvider) provider;
    final List<String> activatedTracers;
    if (logger.isLoggable(Level.CONFIG)) {
      activatedTracers = new ArrayList<>();
    } else {
      activatedTracers = null;
    }
    tracerSdkComponentRegistry
        .getComponents()
        .forEach(
            sdkTracer -> {
              try {
                InstrumentationScopeInfo instrumentationScopeInfo =
                    getInstrumentationScopeInfo(sdkTracer);
                TracerConfig tConfig = getTracerConfig(finalProvider, instrumentationScopeInfo);
                Field tracerEnabledField = sdkTracer.getClass().getDeclaredField("tracerEnabled");
                tracerEnabledField.setAccessible(true);
                // Update is synced but the reader is NOT necessarily so this is eventual
                // consistency, takes effect when the application passes a sync boundary
                synchronized (sdkTracer) {
                  tracerEnabledField.set(sdkTracer, tConfig.isEnabled());
                }
                if (logger.isLoggable(Level.CONFIG)) {
                  String name = instrumentationScopeInfo.getName();
                  if (name.startsWith(INSTRUMENTATION_NAME_PREPEND)) {
                    name = name.substring(INSTRUMENTATION_NAME_PREPEND.length());
                  }
                  activatedTracers.add(name);
                  activatedTracers.add(tConfig.isEnabled() ? "enabled" : "disabled");
                }
              } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
              }
            });
    if (logger.isLoggable(Level.CONFIG)) {
      logger.log(Level.CONFIG, "Activated Tracers: " + activatedTracers);
    }
  }

  static {
    // will refactor this when DynamicInstrumentation class becomes mostly empty
    DynamicConfigurationPropertyChecker.startCheckerThread();
  }
}
