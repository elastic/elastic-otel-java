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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ElasticSpanExporter implements SpanExporter {

  private final SpanExporter delegate;

  private ConcurrentHashMap<SpanContext, ElasticBreakdownMetrics.SpanContextData> storage;

  public ElasticSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
    this.storage = new ConcurrentHashMap<>();
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    // shortcut in the rare case where no filtering is required
    if (storage.isEmpty()) {
      return delegate.export(spans);
    }

    List<SpanData> toSend = new ArrayList<>(spans.size());
    for (SpanData span : spans) {
      SpanContext spanContext = span.getSpanContext();
      ElasticBreakdownMetrics.SpanContextData data = storage.remove(spanContext);
      if (data == null) {
        toSend.add(span);
      } else {
        toSend.add(
            new DelegatingSpanData(span) {
              @Override
              public Attributes getAttributes() {
                return span.getAttributes().toBuilder()
                    .put(ElasticAttributes.SELF_TIME_ATTRIBUTE, data.getSelfTime())
                    .build();
              }

              @Override
              public Resource getResource() {
                Resource original = span.getResource();
                return Resource.create(original.getAttributes(), original.getSchemaUrl());
              }
            });
      }
    }

    return delegate.export(toSend);
  }

  public void report(SpanContext spanContext, ElasticBreakdownMetrics.SpanContextData data) {
    this.storage.put(spanContext, data);
  }

  @Override
  public CompletableResultCode flush() {
    storage.clear();
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
