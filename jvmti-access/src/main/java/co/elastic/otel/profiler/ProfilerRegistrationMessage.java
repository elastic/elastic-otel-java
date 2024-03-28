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

public class ProfilerRegistrationMessage implements ProfilerMessage {

  static final int TYPE_ID = 2;

  long samplesDelayMillis;
  String hostId;

  /**
   * A sane upper bound of the usual time taken in milliseconds by the profiling host agent between
   * the collection of a stacktrace and it being written to the messaging socket. Note that this
   * value doesn't need to be a hard a guarantee, but it should be the 99% case so that profiling
   * data isn't distorted in the expected case.
   */
  public long getSamplesDelayMillis() {
    return samplesDelayMillis;
  }

  /**
   * The OpenTelemetry SemConv host.id resource attribute used by the profiling host agent for the
   * captured profiling data.
   */
  public String getHostId() {
    return hostId;
  }
}
