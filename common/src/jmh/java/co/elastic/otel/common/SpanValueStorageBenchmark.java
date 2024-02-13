package co.elastic.otel.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.FieldBackedSpanValueStorageProvider;
import io.opentelemetry.sdk.trace.SdkSpan;
import java.util.ArrayList;
import java.util.List;
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
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class SpanValueStorageBenchmark {

  @Benchmark
  public void setSpanValue(BenchState state) {
    state.storageProvider
        .get(new SdkSpan(), true)
        .set(state.spanValue, state.valueToSet);
  }

  @Benchmark
  public void getSpanValue(BenchState state, Blackhole bh) {
    Object result = null;
    SpanValueStorage spanValueStorage = state.storageProvider.get(state.theSpan, false);
    if (spanValueStorage != null) {
      result = spanValueStorage.get(state.spanValue);
    }
    bh.consume(result);
  }

  @State(Scope.Benchmark)
  public static class BenchState {

    @Param({"false", "true"})
    boolean fieldBacked;

    @Param({"false", "true"})
    boolean isDense;

    Span theSpan;

    private static final SpanValue<Object> DENSE_SPV = SpanValue.createDense();

    SpanValue<Object> spanValue;

    SpanValueStorageProvider storageProvider;

    Object valueToSet = new Object();

    List<Span> otherSpans = new ArrayList<>();

    @Setup(Level.Iteration)
    public void init(Blackhole blackhole) {
      theSpan = new SdkSpan();
      spanValue = isDense ? DENSE_SPV : SpanValue.createSparse();
      storageProvider = fieldBacked ? FieldBackedSpanValueStorageProvider.INSTANCE
          : new SpanValueStorageProvider.MapBacked();

      storageProvider.get(theSpan, true).set(spanValue, new Object());

      for (int i = 0; i < 32; i++) {
        Span otherSpan = new SdkSpan();
        storageProvider.get(otherSpan, true).set(spanValue, new Object());
        otherSpans.add(otherSpan);
      }
    }

  }
}
