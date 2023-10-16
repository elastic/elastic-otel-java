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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ElasticSpanExporter implements SpanExporter {

  private final SpanExporter delegate;

  private final ConcurrentHashMap<SpanContext, AttributesBuilder> attributes;

  public ElasticSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
    this.attributes = new ConcurrentHashMap<>();
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    // shortcut in the rare case where no filtering is required
    if (attributes.isEmpty()) {
      return delegate.export(spans);
    }

    List<SpanData> toSend = new ArrayList<>(spans.size());
    for (SpanData span : spans) {
      SpanContext spanContext = span.getSpanContext();
      AttributesBuilder extraAttributes = attributes.remove(spanContext);
      if (extraAttributes == null) {
        toSend.add(span);
      } else {
        toSend.add(
                new DelegatingSpanData(span) {
                  @Override
                  public Attributes getAttributes() {
                    return span.getAttributes().toBuilder().putAll(extraAttributes.build()).build();
                  }
                });
      }
    }

    return delegate.export(toSend);
  }

  public <T> void addAttribute(SpanContext spanContext, AttributeKey<T> key, T value) {
    attributes.compute(spanContext, (k, builder) -> {
      if (builder == null) {
        builder = Attributes.builder();
      }
      builder.put(key, value);
      return builder;
    });
  }

  @Override
  public CompletableResultCode flush() {
    attributes.clear();
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
