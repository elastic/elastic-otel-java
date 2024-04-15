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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class UniversalProfilingIntegrationBenchmark {

  @Benchmark
  public void activateAndDeactivate(OtelState state, Blackhole bh) throws Exception {
    // This benchmark performs four changes to the native memory: two span activations and two
    // deactivations
    bh.consume(Context.current());
    try (io.opentelemetry.context.Scope s = state.rootSpanContext.makeCurrent()) {
      bh.consume(Context.current());
      try (io.opentelemetry.context.Scope s2 = state.childSpanContext.makeCurrent()) {
        bh.consume(Context.current());
      }
      bh.consume(Context.current());
    }
    bh.consume(Context.current());
  }

  @Benchmark
  public void contextSwitchWithoutSpanChange(OtelState state, Blackhole bh) throws Exception {
    bh.consume(Context.current());
    try (io.opentelemetry.context.Scope s = Context.root().makeCurrent()) {
      bh.consume(Context.current());
    }
    bh.consume(Context.current());
  }

  @State(Scope.Benchmark)
  public static class OtelState {

    @Param({"false", "true"})
    boolean universalProfilerProcessorActive;

    SdkTracerProvider tracerProvider;
    Context rootSpanContext;
    Context childSpanContext;

    @Setup(Level.Iteration)
    public void init(Blackhole blackhole) {
      BlackholeSpanProcessor spanProcessor = new BlackholeSpanProcessor(blackhole);
      SdkTracerProviderBuilder builder = SdkTracerProvider.builder();
      if (universalProfilerProcessorActive) {
        Resource res =
            Resource.builder().put(ResourceAttributes.SERVICE_NAME, "benchmark-service").build();
        builder.addSpanProcessor(UniversalProfilingProcessor.builder(spanProcessor, res).build());
      }
      tracerProvider = builder.build();

      Tracer tracer = tracerProvider.get("benchmark-spans");
      Span span = tracer.spanBuilder("root").startSpan();
      span.end();
      rootSpanContext = Context.current().with(span);

      Span child = tracer.spanBuilder("child").setParent(rootSpanContext).startSpan();
      span.end();
      childSpanContext = rootSpanContext.with(child);
    }

    @TearDown(Level.Iteration)
    public void destroy() {
      tracerProvider.close();
    }
  }
}
