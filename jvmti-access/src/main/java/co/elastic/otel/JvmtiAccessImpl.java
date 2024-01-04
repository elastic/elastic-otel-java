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

import java.nio.ByteBuffer;

public class JvmtiAccessImpl {

  static native int destroy0();

  /**
   * @param threadBuffer the buffer whose address will get stored in the native thread-local-storage for APM <-> profiling correlation
   */
  static native void setThreadProfilingCorrelationBuffer0(ByteBuffer threadBuffer);

  /**
   * @param byteBuffer the buffer whose address will get stored in the native global variable for APM <-> profiling correlation
   */
  static native void setProcessProfilingCorrelationBuffer0(ByteBuffer byteBuffer);

  /**
   * ONLY FOR TESTING!
   * Creates a new bytebuffer for reading the currently configured thread local correlation buffer.
   * This buffer points to the same memory address as the buffer configured via setThreadProfilingCorrelationBuffer0.
   */
  static native ByteBuffer createThreadProfilingCorrelationBufferAlias(long capacity);

  /**
   * ONLY FOR TESTING!
   * Creates a new bytebuffer for reading the currently configured process local correlation buffer.
   * This buffer points to the same memory address as the buffer configured via setProcessProfilingCorrelationBuffer0.
   */
  static native ByteBuffer createProcessProfilingCorrelationBufferAlias(long capacity);

}
