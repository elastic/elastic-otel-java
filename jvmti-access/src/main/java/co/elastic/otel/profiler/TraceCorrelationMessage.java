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
package co.elastic.otel.profiler;

import java.util.Arrays;

public class TraceCorrelationMessage implements ProfilerMessage {

  static final int TYPE_ID = 1;
  final byte[] traceId = new byte[16];
  final byte[] localRootSpanId = new byte[8];
  final byte[] stackTraceId = new byte[16];
  int sampleCount;

  /**
   * @return the 16 byte trace id for which a CPU profiling sample was taken
   */
  public byte[] getTraceId() {
    return traceId;
  }

  /**
   * @return the 8 byte span id of the local root span for which a CPU profiling sample was taken
   */
  public byte[] getLocalRootSpanId() {
    return localRootSpanId;
  }

  /**
   * @return the 8 byte id of the stack trace observed by the profiler
   */
  public byte[] getStackTraceId() {
    return stackTraceId;
  }

  /**
   * @return the total number of samples with the given trace-id, local-root-span-id and
   *     stacktrace-id observed since the last report
   */
  public int getSampleCount() {
    return sampleCount;
  }

  @Override
  public String toString() {
    return "TraceCorrelationMessage{"
        + "traceId="
        + Arrays.toString(traceId)
        + ", localRootSpanId="
        + Arrays.toString(localRootSpanId)
        + ", stackTraceId="
        + Arrays.toString(stackTraceId)
        + ", sampleCount="
        + sampleCount
        + '}';
  }
}
