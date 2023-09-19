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
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ElasticExtension {

  public static final ElasticExtension INSTANCE = new ElasticExtension();

  private final ElasticProfiler profiler;
  private final ElasticBreakdownMetrics breakdownMetrics;
  private final ElasticSpanProcessor spanProcessor;
  private ElasticSpanExporter spanExporter;

  private ElasticExtension() {
    this.profiler = new ElasticProfiler();
    this.breakdownMetrics = new ElasticBreakdownMetrics();
    this.spanProcessor = new ElasticSpanProcessor(profiler, breakdownMetrics);
  }

  public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
    profiler.registerOpenTelemetry(openTelemetry);
    breakdownMetrics.registerOpenTelemetry(openTelemetry);
  }

  public SpanProcessor getSpanProcessor() {
    return spanProcessor;
  }

  public ContextStorage wrapContextStorage(ContextStorage toWrap) {
    return new ElasticContextStorage(toWrap, profiler);
  }

  public SpanExporter wrapSpanExporter(SpanExporter toWrap) {
    // make the sampling profiler directly use the original exporter
    profiler.registerExporter(toWrap);
    spanExporter = new ElasticSpanExporter(toWrap);
    breakdownMetrics.registerSpanExporter(spanExporter);
    return spanExporter;
  }
}
