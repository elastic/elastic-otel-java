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

import static co.elastic.otel.dynamicconfig.DynamicInstrumentation.setProviderTracerConfigurator;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.internal.ScopeConfigurator;
import io.opentelemetry.sdk.trace.internal.TracerConfig;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicConfiguration {
  static final String INSTRUMENTATION_NAME_PREPEND = "io.opentelemetry.";
  private static final String ALL_INSTRUMENTATION = "_ALL_";
  private static final String ALL_INSTRUMENTATION_FULL_NAME =
      INSTRUMENTATION_NAME_PREPEND + ALL_INSTRUMENTATION;
  private static final DynamicConfiguration INSTANCE = new DynamicConfiguration();
  private static final Logger logger = Logger.getLogger(DynamicConfiguration.class.getName());

  public static DynamicConfiguration getInstance() {
    return INSTANCE;
  }

  public static final String DISABLE_SEND_OPTION = "elastic.otel.java.experimental.disable_send";
  public static final String INSTRUMENTATION_DISABLE_OPTION =
      "elastic.otel.java.experimental.disable_instrumentations";

  private Boolean recoverySendSpansState;
  private Boolean recoverySendLogsState;
  private Boolean recoverySendMetricsState;
  private final ConcurrentMap<String, Boolean> alreadyDeactivated = new ConcurrentHashMap<>();
  private String cachedDeactivationList;

  private void initSendingStates() {
    if (recoverySendSpansState == null) {
      if (BlockableSpanExporter.getInstance() == null) {
        logger.log(
            Level.WARNING,
            "BlockableSpanExporter.getInstance() == null which is unexpected unless this is a test");
      } else {
        recoverySendSpansState = BlockableSpanExporter.getInstance().sendingSpans();
      }
    }
    if (recoverySendMetricsState == null) {
      if (BlockableMetricExporter.getInstance() == null) {
        logger.log(
            Level.WARNING,
            "BlockableMetricExporter.getInstance() == null which is unexpected unless this is a test");
      } else {
        recoverySendMetricsState = BlockableMetricExporter.getInstance().sendingMetrics();
      }
    }
    if (recoverySendLogsState == null) {
      if (BlockableLogRecordExporter.getInstance() == null) {
        logger.log(
            Level.WARNING,
            "BlockableLogRecordExporter.getInstance() == null which is unexpected unless this is a test");
      } else {
        recoverySendLogsState = BlockableLogRecordExporter.getInstance().sendingLogs();
      }
    }
  }

  /** Can be executed repeatedly even if sending is currently stopped */
  public void stopAllSending() {
    initSendingStates();
    if (recoverySendSpansState != null) {
      BlockableSpanExporter.getInstance().setSendingSpans(false);
    }
    if (recoverySendMetricsState != null) {
      BlockableMetricExporter.getInstance().setSendingMetrics(false);
    }
    if (recoverySendLogsState != null) {
      BlockableLogRecordExporter.getInstance().setSendingLogs(false);
    }
  }

  /** Can be executed repeatedly even if sending is currently proceeding */
  public void restartAllSending() {
    initSendingStates();
    if (recoverySendSpansState != null) {
      BlockableSpanExporter.getInstance().setSendingSpans(recoverySendSpansState);
    }
    if (recoverySendMetricsState != null) {
      BlockableMetricExporter.getInstance().setSendingMetrics(recoverySendMetricsState);
    }
    if (recoverySendLogsState != null) {
      BlockableLogRecordExporter.getInstance().setSendingLogs(recoverySendLogsState);
    }
  }

  public void reenableTracesFor(String instrumentationName) {
    UpdatableConfigurator.INSTANCE.put(
        InstrumentationScopeInfo.create(INSTRUMENTATION_NAME_PREPEND + instrumentationName),
        TracerConfig.enabled());
    setProviderTracerConfigurator(
        GlobalOpenTelemetry.getTracerProvider(), UpdatableConfigurator.INSTANCE);
  }

  public void disableTracesFor(String instrumentationName) {
    UpdatableConfigurator.INSTANCE.put(
        InstrumentationScopeInfo.create(INSTRUMENTATION_NAME_PREPEND + instrumentationName),
        TracerConfig.disabled());
    setProviderTracerConfigurator(
        GlobalOpenTelemetry.getTracerProvider(), UpdatableConfigurator.INSTANCE);
  }

  public void disableAllTraces() {
    disableTracesFor(ALL_INSTRUMENTATION);
  }

  public void stopDisablingAllTraces() {
    reenableTracesFor(ALL_INSTRUMENTATION);
  }

  // okay to synchronize as this should only be called after multi-second intervals and
  // additionally only called from threads which are not doing anything application-blocking
  public synchronized void deactivateInstrumentations(String deactivateList) {
    // Avoid doing anything if the deactivateList hasn't changed -
    // this is just a GC optimization to avoid creating new objects in the most common case
    if (Objects.equals(deactivateList, cachedDeactivationList)) {
      return;
    }
    cachedDeactivationList = deactivateList;

    // Algorithm:
    // 1. If the list is empty, then everything alreadyDeactivated needs to be re-activated
    // 2. Otherwise
    // 2a. everything in both deactivateList and alreadyDeactivated is ignored (already deactivated)
    // 2b. everything in deactivateList not in alreadyDeactivated is deactivated
    // 2c. everything in alreadyDeactivated not in deactivateList is re-eactivated
    if (deactivateList == null || deactivateList.trim().isEmpty()) {
      // Applying (1) - keySet.remove() is a valid concurrent mutation here within the loop
      Set<String> keySet = alreadyDeactivated.keySet();
      for (String instrumentation : keySet) {
        DynamicConfiguration.getInstance().reenableTracesFor(instrumentation);
        alreadyDeactivated.remove(instrumentation);
      }
    } else {
      // Applying (2)
      Deactivations deactivations =
          new Deactivations(
              convertToSet(deactivateList), new HashSet<>(alreadyDeactivated.keySet()));
      deactivations.applyDeactivations(alreadyDeactivated);
    }
  }

  private static Set<String> convertToSet(String deactivateList) {
    Set<String> theSet = new HashSet<>();
    if (deactivateList != null && !deactivateList.trim().isEmpty()) {
      for (String toBeDisabled : deactivateList.split(",")) {
        theSet.add(toBeDisabled.trim());
      }
    }
    return theSet;
  }

  public static class Deactivations {
    final Set<String> instrumentationsToReactivate;
    final Set<String> instrumentationsToDeactivate;

    public <E> Deactivations(Set<String> deactivateList, Set<String> alreadyDeactivated) {
      instrumentationsToReactivate = new HashSet<>();
      instrumentationsToDeactivate = new HashSet<>();
      for (String instrumentation : deactivateList) {
        if (!alreadyDeactivated.contains(instrumentation)) {
          // Requested to deactivate this and it's not already deactivated, so
          instrumentationsToDeactivate.add(instrumentation);
        }
      }
      for (String instrumentation : alreadyDeactivated) {
        if (!deactivateList.contains(instrumentation)) {
          // Currently deactivated but now NOT requested to be deactivated, so
          instrumentationsToReactivate.add(instrumentation);
        }
      }
    }

    public void applyDeactivations(ConcurrentMap<String, Boolean> alreadyDeactivated) {
      for (String instrumentation : instrumentationsToReactivate) {
        DynamicConfiguration.getInstance().reenableTracesFor(instrumentation);
        alreadyDeactivated.remove(instrumentation);
      }
      for (String instrumentation : instrumentationsToDeactivate) {
        DynamicConfiguration.getInstance().disableTracesFor(instrumentation);
        alreadyDeactivated.put(instrumentation, Boolean.TRUE);
      }
    }
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
