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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class ElasticMetricExporter implements MetricExporter {
  private static final AtomicReference<ElasticMetricExporter> INSTANCE = new AtomicReference<>();

  private volatile boolean sendingMetrics = true;
  private final MetricExporter delegate;

  public static ElasticMetricExporter getInstance() {
    return INSTANCE.get();
  }

  static ElasticMetricExporter createCustomInstance(MetricExporter exporter) {
    INSTANCE.set(new ElasticMetricExporter(exporter));
    return INSTANCE.get();
  }

  private ElasticMetricExporter(MetricExporter delegate) {
    this.delegate = delegate;
  }

  public void setSendingMetrics(boolean send) {
    sendingMetrics = send;
  }

  public boolean sendingMetrics() {
    return sendingMetrics;
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.DELTA;
  }

  @Override
  public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
    return delegate.getDefaultAggregation(instrumentType);
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    if (sendingMetrics) {
      return delegate.export(metrics);
    } else {
      return CompletableResultCode.ofFailure();
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
