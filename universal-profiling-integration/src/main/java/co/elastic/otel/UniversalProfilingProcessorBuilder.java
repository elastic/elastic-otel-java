package co.elastic.otel;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.function.LongSupplier;

public class UniversalProfilingProcessorBuilder {


  private final Resource resource;
  private final SpanProcessor nextProcessor;
  private Duration spanDelay = Duration.ofSeconds(10);

  private LongSupplier nanoClock = System::nanoTime;

  private int bufferSize = 8096;

  UniversalProfilingProcessorBuilder(SpanProcessor next, Resource resource) {
    this.resource = resource;
    this.nextProcessor = next;
  }

  public UniversalProfilingProcessor build() {
    return new UniversalProfilingProcessor(nextProcessor, resource, bufferSize, spanDelay,
        nanoClock);
  }

  UniversalProfilingProcessorBuilder clock(LongSupplier nanoClock) {
    this.nanoClock = nanoClock;
    return this;
  }

  UniversalProfilingProcessorBuilder spanDelay(Duration delay) {
    this.spanDelay = delay;
    return this;
  }

  UniversalProfilingProcessorBuilder bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

}
