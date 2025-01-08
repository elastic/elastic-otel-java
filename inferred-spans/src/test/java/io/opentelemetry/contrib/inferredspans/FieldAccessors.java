package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.contrib.inferredspans.internal.SamplingProfiler;

public class FieldAccessors {

  public static SamplingProfiler getProfiler(InferredSpansProcessor processor) {
    return processor.profiler;
  }
}
