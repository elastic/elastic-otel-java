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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticBreakdownMetrics {

  private final ConcurrentHashMap<SpanContext, BreakdownData> elasticSpanData;

  private ElasticSpanExporter spanExporter;

  private LongCounter breakDownCounter;

  // sidecar object we store for every span
  private static class BreakdownData {

    private ReadableSpan localRoot;

    private final ChildDuration childDuration;
    private long selfTime;

    public BreakdownData(ReadableSpan localRoot, long start) {
      this.localRoot = localRoot;
      this.childDuration = new ChildDuration(start);
      this.selfTime = Long.MIN_VALUE;
    }

    public long getSelfTime() {
      if (selfTime < 0) {
        throw new IllegalStateException("invalid self time");
      }
      return selfTime;
    }

    private void setSelfTime(long selfTime) {
      this.selfTime = selfTime;
    }

    public String getLocalRootSpanType() {
      // use a dummy heuristic that matches current transaction.type
      return "request";
    }

    public String getLocalRootSpanName() {
      return localRoot.getName();
    }
  }

  public ElasticBreakdownMetrics() {
    elasticSpanData = new ConcurrentHashMap<>();
  }

  public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
    Meter meter = openTelemetry.getMeterProvider().meterBuilder("elastic.span_breakdown").build();
    breakDownCounter = meter.counterBuilder("elastic.span_breakdown").build();
  }

  public void onSpanStart(Context parentContext, ReadWriteSpan span) {
    // unfortunately we can't cast to SdkSpan as it's loaded in AgentClassloader and the extension
    // is loaded
    // in the ExtensionClassloader, hence we can't use the package-protected
    // SdkSpan#getStartEpochNanos method
    //
    // However, adding accessors to the start/and timestamps to the ReadableSpan interface could be
    // something we
    // could attempt to contribute.
    long spanStart = span.toSpanData().getStartEpochNanos();

    SpanContext spanContext = span.getSpanContext();
    SpanContext localRootSpanContext;

    // We can use this because we have a read write span here
    // alternatively we could have resolved the parent span on the parent context to get parent span
    // context.
    if (isRootSpanParent(span.getParentSpanContext())) {
      // the span is a local root span
      localRootSpanContext = spanContext;

      elasticSpanData.put(spanContext, new BreakdownData(span, spanStart));

    } else {
      ReadableSpan parentSpan = getReadableSpanFromContext(parentContext);
      Objects.requireNonNull(parentSpan);

      // retrieve and store the local root span for later use
      ReadableSpan localRoot = lookupLocalRootSpan(parentSpan);
      localRootSpanContext = localRoot.getSpanContext();
      if (localRootSpanContext.isValid()) {
        elasticSpanData.put(spanContext, new BreakdownData(localRoot, spanStart));
      }

      // update direct parent span child durations for self-time
      ChildDuration parentChildDuration =
          elasticSpanData.get(span.getParentSpanContext()).childDuration;
      if (parentChildDuration != null) {
        parentChildDuration.startChild(spanStart);
      }

      System.out.printf(
          "start of child span %s, parent = %s, root = %s%n",
          spanContext.getSpanId(),
          span.getParentSpanContext().getSpanId(),
          localRootSpanContext.getSpanId());
    }

    // we store extra attributes in span for later use, however we can't replace because we don't
    // have access to the
    // attributes of the parent span, only its span context or the write-only Span
    span.setAttribute(ElasticAttributes.IS_LOCAL_ROOT, localRootSpanContext == spanContext);
    span.setAttribute(ElasticAttributes.LOCAL_ROOT_ID, localRootSpanContext.getSpanId());
  }

  private static ReadableSpan getReadableSpanFromContext(Context context) {
    Span span = Span.fromContextOrNull(context);
    return (span instanceof ReadableSpan) ? (ReadableSpan) span : null;
  }

  public void onSpanEnd(ReadableSpan span) {

    SpanContext spanContext = span.getSpanContext();

    // retrieve local root span from storage
    ReadableSpan localRoot = lookupLocalRootSpan(span);

    SpanData spanData = span.toSpanData();

    // children duration for current span
    BreakdownData spanContextData = elasticSpanData.get(spanContext);
    Objects.requireNonNull(spanContextData, "missing elastic span data");

    // update children duration for direct parent
    BreakdownData parentSpanContextData = elasticSpanData.get(span.getParentSpanContext());

    if (parentSpanContextData != null) { // parent might be already terminated
      parentSpanContextData.childDuration.endChild(spanData.getEndEpochNanos());
    }

    long selfTime = spanContextData.childDuration.endSpan(spanData.getEndEpochNanos());

    AttributesBuilder metricAttributes =
        buildCounterAttributes(spanData.getAttributes())
            .put(ElasticAttributes.LOCAL_ROOT_TYPE, spanContextData.getLocalRootSpanType())
            .put(ElasticAttributes.LOCAL_ROOT_NAME, spanContextData.getLocalRootSpanName())
            // put measured metric as span attribute to allow using an ingest pipeline to alter
            // storage
            // ingest pipelines do not have access to _source and thus can't read the metric as-is.
            .put(ElasticAttributes.SELF_TIME, selfTime);

    // unfortunately here we get a read-only span that has already been ended, thus even a cast to
    // ReadWriteSpan
    // does not allow us from adding extra span attributes
    if (spanExporter != null) {
      spanContextData.setSelfTime(selfTime);
      spanExporter.addAttributes(spanContext, attributes -> attributes.put(ElasticAttributes.SELF_TIME, spanContextData.getSelfTime()));
    }

    breakDownCounter.add(selfTime, metricAttributes.build());
    elasticSpanData.remove(spanContext);
  }

  private static AttributesBuilder buildCounterAttributes(Attributes spanAttributes) {
    AttributesBuilder builder =
        Attributes.builder()
            // default to app/internal unless other span attributes
            .put(ElasticAttributes.SPAN_TYPE, "app")
            .put(ElasticAttributes.SPAN_SUBTYPE, "internal");

    spanAttributes.forEach(
        (k, v) -> {
          String key = k.getKey();
          if (AttributeType.STRING.equals(k.getType())) {
            int index = key.indexOf(".system");
            if (index > 0) {
              builder.put(ElasticAttributes.SPAN_TYPE, key.substring(0, index));
              builder.put(ElasticAttributes.SPAN_SUBTYPE, v.toString());
            }
          }
        });
    return builder;
  }

  private ReadableSpan lookupLocalRootSpan(ReadableSpan span) {
    BreakdownData spanContextData = elasticSpanData.get(span.getSpanContext());
    return spanContextData != null ? spanContextData.localRoot : (ReadableSpan) Span.getInvalid();
  }

  private static boolean isRootSpanParent(SpanContext parentSpanContext) {
    return !parentSpanContext.isValid() || parentSpanContext.isRemote();
  }

  public void registerSpanExporter(ElasticSpanExporter spanExporter) {
    this.spanExporter = spanExporter;
  }

  private static class ChildDuration {

    public static final Clock clock = Clock.getDefault();

    private final AtomicInteger activeChildren;

    private final long startEpochNanos;

    // timestamp of the 1st child start
    private long childStartEpoch;

    // duration for which there was at least child span executing
    private long childDuration;

    public ChildDuration(long startEpochNanos) {
      this.activeChildren = new AtomicInteger();
      this.startEpochNanos = startEpochNanos;
    }

    public void startChild(long startEpochNanos) {
      int count;
      synchronized (this) {
        count = activeChildren.incrementAndGet();
        if (count == 1) {
          childStartEpoch = startEpochNanos;
        }
      }
      System.out.printf("start child span, count = %d%n", count);
    }

    public void endChild(long endEpochNanos) {

      int count;
      synchronized (this) {
        count = activeChildren.decrementAndGet();
        if (count == 0) {
          childDuration += (endEpochNanos - childStartEpoch);
          childStartEpoch = -1L;
        }
      }
    }

    /**
     * @param endEpochNanos span end timestamp
     * @return span self time
     */
    public long endSpan(long endEpochNanos) {
      synchronized (this) {
        if (childStartEpoch > 0) {
          childDuration += (clock.now() - childStartEpoch);
        }
      }
      return endEpochNanos - startEpochNanos - childDuration;
    }
  }
}
