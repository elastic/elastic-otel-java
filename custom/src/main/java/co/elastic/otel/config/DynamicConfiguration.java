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

import co.elastic.otel.ElasticLogRecordExporter;
import co.elastic.otel.ElasticMetricExporter;
import co.elastic.otel.ElasticSpanExporter;

public class DynamicConfiguration {
  private static DynamicConfiguration INSTANCE = new DynamicConfiguration();

  public static DynamicConfiguration getInstance() {
    return INSTANCE;
  }

  public static final String DISABLE_SEND_OPTION = "elastic.otel.java.disable_send";

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

  public void stopInstrumentation(String instrumentationName) {
    DynamicInstrumentation.disableTracesFor(instrumentationName);
  }

  public void restartInstrumentation(String instrumentationName) {
    DynamicInstrumentation.reenableTracesFor(instrumentationName);
  }

  // TODO these are in a separate PR, so add after that is merged
  //  public void stopAllInstrumentation() {
  //    DynamicInstrumentation.disableAllTraces();
  //  }
  //
  //  public void restartAllInstrumentation() {
  //    DynamicInstrumentation.stopDisablingAllTraces();
  //  }
}
