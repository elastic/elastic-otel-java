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
  private boolean virtualThreadSupportEnabled = true;

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
        virtualThreadSupportEnabled,
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

  /**
   * The extension needs to buffer ended local-root spans for a short duration to ensure that all of
   * its profiling data has been received. This configuration options configures the buffer size in
   * number of spans. The higher the number of local root spans per second, the higher this buffer
   * size should be set. The extension will log a warning if it is not capable of buffering a span
   * due to insufficient buffer size. This will cause the span to be exported immediately instead
   * with possibly incomplete profiling correlation data.
   */
  public UniversalProfilingProcessorBuilder bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  /**
   * The extension needs to bind a socket to a file for communicating with the universal profiling
   * host agent. By default, this socket will be placed in the java.io.tmpdir. This configuration
   * option can be used to change the location. Note that the total path name (including the socket)
   * must not exceed 100 characters due to OS restrictions.
   */
  public UniversalProfilingProcessorBuilder socketDir(String path) {
    this.socketDir = path;
    return this;
  }

  /**
   * Virtual threads need some extra work for correlation: On mount/unmount the span/trace context
   * of the platform thread needs to be kept in sync. This is done by hooking on to JVMTI-events.
   * This option allows to disable support for virtual threads in case this mechanism causes any problems.
   */
  public UniversalProfilingProcessorBuilder virtualThreadSupportEnabled(boolean enable) {
    this.virtualThreadSupportEnabled = enable;
    return this;
  }
}
