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
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class ElasticSpanExporter implements SpanExporter {
  private static final AtomicReference<ElasticSpanExporter> INSTANCE = new AtomicReference<>();

  private volatile boolean sendingSpans = true;
  private final SpanExporter delegate;

  public static ElasticSpanExporter getInstance() {
    return INSTANCE.get();
  }

  static ElasticSpanExporter createCustomInstance(SpanExporter exporter) {
    INSTANCE.set(new ElasticSpanExporter(exporter));
    return INSTANCE.get();
  }

  private ElasticSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  public void setSendingSpans(boolean send) {
    sendingSpans = send;
  }

  public boolean sendingSpans() {
    return sendingSpans;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (sendingSpans) {
      return delegate.export(spans);
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
}
