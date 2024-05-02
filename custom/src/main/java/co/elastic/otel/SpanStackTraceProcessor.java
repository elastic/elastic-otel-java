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

import co.elastic.otel.common.AbstractSimpleChainingSpanProcessor;
import co.elastic.otel.common.ElasticAttributes;
import co.elastic.otel.common.MutableSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
public class SpanStackTraceProcessor extends AbstractSimpleChainingSpanProcessor {

  private static final Logger logger = Logger.getLogger(SpanStackTraceProcessor.class.getName());

  private final long minSpanDurationNanos;

  public SpanStackTraceProcessor(SpanProcessor next, long minSpanDurationNanos) {
    super(next);
    this.minSpanDurationNanos = minSpanDurationNanos;
    logger.log(
        Level.FINE,
        "Stack traces will be added to spans with a minimum duration of {0} nanos",
        minSpanDurationNanos);
  }

  @Override
  protected boolean requiresStart() {
    return false;
  }

  @Override
  protected boolean requiresEnd() {
    return true;
  }

  @Override
  protected ReadableSpan doOnEnd(ReadableSpan span) {
    if (span.getLatencyNanos() < minSpanDurationNanos) {
      return span;
    }
    if (span.getAttribute(CodeIncubatingAttributes.CODE_STACKTRACE) != null) {
      // Span already has a stacktrace, do not override
      return span;
    }
    Boolean isInferred = span.getAttribute(ElasticAttributes.IS_INFERRED);
    if (isInferred != null && isInferred) {
      // Do not add a stacktrace for inferred spans: If a good one was available
      // it would have been added by the module creating this span
      return span;
    }
    MutableSpan mutableSpan = MutableSpan.makeMutable(span);

    String stacktrace = generateSpanEndStacktrace();
    mutableSpan.setAttribute(CodeIncubatingAttributes.CODE_STACKTRACE, stacktrace);
    return mutableSpan;
  }

  private static String generateSpanEndStacktrace() {
    Throwable exception = new Throwable();
    StringWriter stringWriter = new StringWriter();
    try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
      exception.printStackTrace(printWriter);
    }
    return removeInternalFrames(stringWriter.toString());
  }

  private static String removeInternalFrames(String stackTrace) {
    String lastInternal = "at io.opentelemetry.sdk.trace.SdkSpan.end";

    int idx = stackTrace.lastIndexOf(lastInternal);
    if (idx == -1) {
      // should usually not happen, this means that the span processor was called from somewhere
      // else
      return stackTrace;
    }
    int nextNewLine = stackTrace.indexOf('\n', idx);
    if (nextNewLine == -1) {
      nextNewLine = stackTrace.length() - 1;
    }
    return stackTrace.substring(nextNewLine + 1);
  }
}
