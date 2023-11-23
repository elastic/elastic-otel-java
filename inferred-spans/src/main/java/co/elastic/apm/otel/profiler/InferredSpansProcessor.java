package co.elastic.apm.otel.profiler;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class InferredSpansProcessor implements SpanProcessor {

  private static final Logger logger = Logger.getLogger(InferredSpansProcessor.class.getName());

  public static final String TRACER_NAME = "elastic-inferred-spans";

  //Visible for testing
  final SamplingProfiler profiler;

  private Tracer tracer;

  InferredSpansProcessor(
      InferredSpansConfiguration config,
      NanoClock clock,
      boolean startScheduledProfiling,
      @Nullable File activationEventsFile,
      @Nullable File jfrFile
  ) {
    profiler = new SamplingProfiler(config, clock, this::getTracer, activationEventsFile, jfrFile);
    if (startScheduledProfiling) {
      profiler.start();
    }
  }

  public static InferredSpansProcessorBuilder builder() {
    return new InferredSpansProcessorBuilder();
  }

  /**
   * @param provider the provider to use. Null means that {@link GlobalOpenTelemetry} will be used lazily.
   */
  public synchronized void setTracerProvider(TracerProvider provider) {
    tracer = provider.get(TRACER_NAME);
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    profiler.getClock().onSpanStart(span, parentContext);
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    CompletableResultCode result = new CompletableResultCode();
    logger.fine("Stopping Inferred Spans Processor");
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        profiler.stop();
        result.succeed();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to stop Inferred Spans Processor", e);
        result.fail();
      }
    });
    return result;
  }

  private Tracer getTracer() {
    if (tracer == null) {
      synchronized (this) {
        if (tracer == null) {
          setTracerProvider(GlobalOpenTelemetry.get().getTracerProvider());
        }
      }
    }
    return tracer;
  }

}
