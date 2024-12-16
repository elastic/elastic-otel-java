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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import javax.annotation.Nonnull;

public class BlockableMetricExporter implements MetricExporter {
  private static volatile BlockableMetricExporter INSTANCE;

  private volatile boolean sendingMetrics = true;
  private final MetricExporter delegate;

  public static BlockableMetricExporter getInstance() {
    return INSTANCE;
  }

  public static BlockableMetricExporter createCustomInstance(MetricExporter exporter) {
    INSTANCE = new BlockableMetricExporter(exporter);
    return INSTANCE;
  }

  private BlockableMetricExporter(MetricExporter delegate) {
    this.delegate = delegate;
  }

  public void setSendingMetrics(boolean send) {
    sendingMetrics = send;
  }

  public boolean sendingMetrics() {
    return sendingMetrics;
  }

  @Override
  public AggregationTemporality getAggregationTemporality(@Nonnull InstrumentType instrumentType) {
    return delegate.getAggregationTemporality(instrumentType);
  }

  @Override
  public Aggregation getDefaultAggregation(@Nonnull InstrumentType instrumentType) {
    return delegate.getDefaultAggregation(instrumentType);
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<MetricData> metrics) {
    if (sendingMetrics) {
      return delegate.export(metrics);
    } else {
      return CompletableResultCode.ofSuccess();
    }
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
