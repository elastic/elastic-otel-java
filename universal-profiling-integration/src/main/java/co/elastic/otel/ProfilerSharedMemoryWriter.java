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

import co.elastic.otel.common.LocalRootSpan;
import co.elastic.otel.common.util.HexUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.semconv.ServiceAttributes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfilerSharedMemoryWriter {

  private static final Logger log = Logger.getLogger(ProfilerSharedMemoryWriter.class.getName());

  private static final int TLS_MINOR_VERSION_OFFSET = 0;
  private static final int TLS_VALID_OFFSET = 2;
  private static final int TLS_TRACE_PRESENT_OFFSET = 3;
  private static final int TLS_TRACE_FLAGS_OFFSET = 4;
  private static final int TLS_TRACE_ID_OFFSET = 5;
  private static final int TLS_SPAN_ID_OFFSET = 21;
  private static final int TLS_LOCAL_ROOT_SPAN_ID_OFFSET = 29;
  static final int TLS_STORAGE_SIZE = 37;

  private static volatile int writeForMemoryBarrier = 0;

  static ByteBuffer generateProcessCorrelationStorage(
      Resource serviceResource, String socketFilePath) {
    String serviceName = serviceResource.getAttribute(ServiceAttributes.SERVICE_NAME);
    if (serviceName == null) {
      throw new IllegalStateException("A service name must be configured!");
    }
    String environment =
        serviceResource.getAttribute(UniversalProfilingIncubatingAttributes.SERVICE_NAMESPACE);

    ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    buffer.order(ByteOrder.nativeOrder());
    buffer.position(0);

    buffer.putChar((char) 1); // layout-minor-version
    writeUtf8Str(buffer, serviceName);
    writeUtf8Str(buffer, environment == null ? "" : environment);
    writeUtf8Str(buffer, socketFilePath); // socket-file-path
    return buffer;
  }

  private static void writeUtf8Str(ByteBuffer buffer, String str) {
    byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
    buffer.putInt(utf8.length);
    buffer.put(utf8);
  }

  /**
   * This method ensures that all writes which happened prior to this method call are not moved
   * after the method call due to reordering.
   *
   * <p>This is realized based on the Java Memory Model guarantess for volatile variables. Relevant
   * resources:
   *
   * <ul>
   *   <li><a
   *       href="https://stackoverflow.com/questions/17108541/happens-before-relationships-with-volatile-fields-and-synchronized-blocks-in-jav">StackOverflow
   *       topic</a>
   *   <li><a href="https://gee.cs.oswego.edu/dl/jmm/cookbook.html">JSR Compiler Cookbook</a>
   * </ul>
   */
  private static void memoryStoreStoreBarrier() {
    writeForMemoryBarrier = 42;
  }

  static void updateThreadCorrelationStorage(Span newSpan) {
    ByteBuffer tls = UniversalProfilingCorrelation.getCurrentThreadStorage(true, TLS_STORAGE_SIZE);
    // tls might be null if unsupported or something went wrong on initialization
    if (tls != null) {
      // the valid flag is used to signal the host-agent that it is reading incomplete data
      tls.put(TLS_VALID_OFFSET, (byte) 0);
      memoryStoreStoreBarrier();
      tls.putChar(TLS_MINOR_VERSION_OFFSET, (char) 1);

      SpanContext spanCtx = newSpan.getSpanContext();
      if (spanCtx.isValid() && !spanCtx.isRemote()) {
        ReadableSpan localRoot = LocalRootSpan.getFor(newSpan);
        if (localRoot != null) {
          String localRootSpanId = localRoot.getSpanContext().getSpanId();
          tls.put(TLS_TRACE_PRESENT_OFFSET, (byte) 1);
          tls.put(TLS_TRACE_FLAGS_OFFSET, spanCtx.getTraceFlags().asByte());
          HexUtils.writeHexAsBinary(spanCtx.getTraceId(), 0, tls, TLS_TRACE_ID_OFFSET, 16);
          HexUtils.writeHexAsBinary(spanCtx.getSpanId(), 0, tls, TLS_SPAN_ID_OFFSET, 8);
          HexUtils.writeHexAsBinary(localRootSpanId, 0, tls, TLS_LOCAL_ROOT_SPAN_ID_OFFSET, 8);
        } else {
          tls.put(TLS_TRACE_PRESENT_OFFSET, (byte) 0);
          log.log(Level.WARNING, "Cannot propagate trace with unknown local root: {0}", newSpan);
        }
      } else {
        tls.put(TLS_TRACE_PRESENT_OFFSET, (byte) 0);
      }
      memoryStoreStoreBarrier();
      tls.put(TLS_VALID_OFFSET, (byte) 1);
    }
  }
}
