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

import static co.elastic.otel.config.DynamicInstrumentation.updateTracerConfigurations;

import co.elastic.otel.ElasticLogRecordExporter;
import co.elastic.otel.ElasticMetricExporter;
import co.elastic.otel.ElasticSpanExporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.internal.ScopeConfigurator;
import io.opentelemetry.sdk.trace.internal.TracerConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DynamicConfiguration {
  static final String INSTRUMENTATION_NAME_PREPEND = "io.opentelemetry.";
  private static final String ALL_INSTRUMENTATION = "_ALL_";
  private static final String ALL_INSTRUMENTATION_FULL_NAME =
      INSTRUMENTATION_NAME_PREPEND + ALL_INSTRUMENTATION;
  private static DynamicConfiguration INSTANCE = new DynamicConfiguration();

  public static DynamicConfiguration getInstance() {
    return INSTANCE;
  }

  public static final String DISABLE_SEND_OPTION = "elastic.otel.java.experimental.disable_send";
  public static final String INSTRUMENTATION_DISABLE_OPTION =
      "elastic.otel.java.experimental.disable_instrumentations";

  private Boolean recoverySendSpansState;
  private Boolean recoverySendLogsState;
  private Boolean recoverySendMetricsState;

  private boolean initSendingStates() {
    if (recoverySendSpansState == null && ElasticSpanExporter.getInstance() != null) {
      recoverySendSpansState = ElasticSpanExporter.getInstance().sendingSpans();
    }
    if (recoverySendMetricsState == null && ElasticMetricExporter.getInstance() != null) {
      recoverySendMetricsState = ElasticMetricExporter.getInstance().sendingMetrics();
    }
    if (recoverySendLogsState == null && ElasticLogRecordExporter.getInstance() != null) {
      recoverySendLogsState = ElasticLogRecordExporter.getInstance().sendingLogs();
    }
    return recoverySendLogsState != null
        && recoverySendMetricsState != null
        && recoverySendSpansState != null
        && recoverySendMetricsState;
  }

  /** Can be executed repeatedly even if sending is currently stopped */
  public void stopAllSending() {
    if (initSendingStates()) {
      ElasticSpanExporter.getInstance().setSendingSpans(false);
      ElasticMetricExporter.getInstance().setSendingMetrics(false);
      ElasticLogRecordExporter.getInstance().setSendingLogs(false);
    }
  }

  /** Can be executed repeatedly even if sending is currently proceeding */
  public void restartAllSending() {
    if (initSendingStates()) {
      ElasticSpanExporter.getInstance().setSendingSpans(recoverySendSpansState);
      ElasticMetricExporter.getInstance().setSendingMetrics(recoverySendMetricsState);
      ElasticLogRecordExporter.getInstance().setSendingLogs(recoverySendLogsState);
    }
  }

  public void reenableTracesFor(String instrumentationName) {
    UpdatableConfigurator.INSTANCE.put(
        InstrumentationScopeInfo.create(INSTRUMENTATION_NAME_PREPEND + instrumentationName),
        TracerConfig.enabled());
    updateTracerConfigurations(GlobalOpenTelemetry.getTracerProvider());
  }

  public void disableTracesFor(String instrumentationName) {
    UpdatableConfigurator.INSTANCE.put(
        InstrumentationScopeInfo.create(INSTRUMENTATION_NAME_PREPEND + instrumentationName),
        TracerConfig.disabled());
    updateTracerConfigurations(GlobalOpenTelemetry.getTracerProvider());
  }

  public void disableAllTraces() {
    disableTracesFor(ALL_INSTRUMENTATION);
  }

  public void stopDisablingAllTraces() {
    reenableTracesFor(ALL_INSTRUMENTATION);
  }

  public static class UpdatableConfigurator implements ScopeConfigurator<TracerConfig> {
    public static final UpdatableConfigurator INSTANCE = new UpdatableConfigurator();
    private final ConcurrentMap<String, TracerConfig> map = new ConcurrentHashMap<>();

    private UpdatableConfigurator() {}

    @Override
    public TracerConfig apply(InstrumentationScopeInfo scopeInfo) {
      // If key "_ALL_" is set to disabled, then always return disabled
      // otherwise fallback to the individual instrumentation
      if (!map.getOrDefault(ALL_INSTRUMENTATION_FULL_NAME, TracerConfig.enabled()).isEnabled()) {
        return TracerConfig.disabled();
      }
      return map.getOrDefault(scopeInfo.getName(), TracerConfig.defaultConfig());
    }

    public void put(InstrumentationScopeInfo scope, TracerConfig tracerConfig) {
      map.put(scope.getName(), tracerConfig);
    }
  }
}
