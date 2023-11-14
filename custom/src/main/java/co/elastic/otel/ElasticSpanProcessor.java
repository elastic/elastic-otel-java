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

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ElasticSpanProcessor implements SpanProcessor {

  private final ElasticProfiler profiler;
  private final ElasticBreakdownMetrics breakdownMetrics;
  private ElasticSpanExporter spanExporter;

  public ElasticSpanProcessor(ElasticProfiler profiler, ElasticBreakdownMetrics breakdownMetrics) {
    this.profiler = profiler;
    this.breakdownMetrics = breakdownMetrics;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    profiler.onSpanStart(parentContext, span);
    breakdownMetrics.onSpanStart(parentContext, span);
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    profiler.onSpanEnd(span);
    breakdownMetrics.onSpanEnd(span);

    captureStackTrace(span);
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public CompletableResultCode shutdown() {
    profiler.shutdown();
    return CompletableResultCode.ofSuccess();
  }

  public void registerSpanExporter(ElasticSpanExporter spanExporter) {
    this.spanExporter = spanExporter;
  }

  private void captureStackTrace(ReadableSpan span) {
    if (spanExporter == null) {
      return;
    }

    // do not overwrite stacktrace if present
    if (span.getAttribute(ElasticAttributes.SPAN_STACKTRACE) == null) {
      Throwable exception = new Throwable();
      StringWriter stringWriter = new StringWriter();
      try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
        exception.printStackTrace(printWriter);
      }

      // TODO should we filter-out the calling code that is within the agent: at least onEnd +
      // captureStackTrace will be included here
      spanExporter.addAttribute(
          span.getSpanContext(), ElasticAttributes.SPAN_STACKTRACE, stringWriter.toString());
    }
  }
}
