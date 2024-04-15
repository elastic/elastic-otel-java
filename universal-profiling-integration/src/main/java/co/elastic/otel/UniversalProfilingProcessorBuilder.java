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

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.LongSupplier;

public class UniversalProfilingProcessorBuilder {

  private final Resource resource;
  private final SpanProcessor nextProcessor;
  private boolean delayActivationAfterProfilerRegistration = true;

  private LongSupplier nanoClock = System::nanoTime;

  private int bufferSize = 8096;

  private String socketDir = System.getProperty("java.io.tmpdir");

  UniversalProfilingProcessorBuilder(SpanProcessor next, Resource resource) {
    this.resource = resource;
    this.nextProcessor = next;
  }

  public UniversalProfilingProcessor build() {
    return new UniversalProfilingProcessor(
        nextProcessor,
        resource,
        bufferSize,
        delayActivationAfterProfilerRegistration,
        socketDir,
        nanoClock);
  }

  UniversalProfilingProcessorBuilder clock(LongSupplier nanoClock) {
    this.nanoClock = nanoClock;
    return this;
  }

  /**
   * If enabled, the profiling integration will remain inactive until the presence of a profiler is
   * actually detected. This safes a bit of overhead in the case no profiler is there.
   *
   * <p>The downside is if the application starts a span immediately after startup, the profiler
   * might not be detected in time and therefore this first span might not be correlated correctly.
   * This can be avoided by setting this option to {@code false}. In this case the {@link
   * UniversalProfilingProcessor} will assume a profiler will be eventually running and start the
   * correlation eagerly.
   */
  public UniversalProfilingProcessorBuilder delayActivationAfterProfilerRegistration(
      boolean value) {
    this.delayActivationAfterProfilerRegistration = value;
    return this;
  }

  public UniversalProfilingProcessorBuilder bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public UniversalProfilingProcessorBuilder socketDir(String path) {
    this.socketDir = path;
    return this;
  }
}
