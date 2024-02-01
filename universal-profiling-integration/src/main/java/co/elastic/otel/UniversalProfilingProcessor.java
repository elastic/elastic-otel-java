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

import co.elastic.otel.common.util.HexUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class UniversalProfilingProcessor implements SpanProcessor {

  private static final int TLS_MINOR_VERSION_OFFSET = 0;
  private static final int TLS_VALID_OFFSET = 2;
  private static final int TLS_TRACE_PRESENT_OFFSET = 3;
  private static final int TLS_TRACE_FLAGS_OFFSET = 4;
  private static final int TLS_TRACE_ID_OFFSET = 5;
  private static final int TLS_SPAN_ID_OFFSET = 21;
  private static final int TLS_LOCAL_ROOT_SPAN_ID_OFFSET = 29;
  static final int TLS_STORAGE_SIZE = 37;

  private static final Logger log = Logger.getLogger(UniversalProfilingProcessor.class.getName());

  private static boolean anyInstanceActive = false;

  public UniversalProfilingProcessor(Resource serviceResource) {
    synchronized (UniversalProfilingProcessor.class) {
      if (anyInstanceActive) {
        throw new IllegalStateException(
            "Another instance has already been started and not stopped yet."
                + " There must be at most one processor of this type active at a time!");
      }
      anyInstanceActive = true;
    }
    populateProcessCorrelationStorage(serviceResource);
    ActivationListener.setProcessor(this);
  }

  private void populateProcessCorrelationStorage(Resource serviceResource) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    buffer.order(ByteOrder.nativeOrder());
    buffer.position(0);

    buffer.putChar((char) 1); // layout-minor-version
    String serviceName = serviceResource.getAttribute(ResourceAttributes.SERVICE_NAME);
    if (serviceName == null) {
      throw new IllegalStateException("A service name must be configured!");
    }
    writeUtf8Str(buffer, serviceName);
    String environment = serviceResource.getAttribute(ResourceAttributes.SERVICE_NAMESPACE);
    writeUtf8Str(buffer, environment == null ? "" : environment);
    // TODO: implement socket connection
    writeUtf8Str(buffer, ""); // socket-file-path

    UniversalProfilingCorrelation.setProcessStorage(buffer);
  }

  private static void writeUtf8Str(ByteBuffer buffer, String str) {
    byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
    buffer.putInt(utf8.length);
    buffer.put(utf8);
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Nullable
  private void onContextChange(@Nullable Context previous, @Nullable Context next) {
    try {
      Span oldSpan = safeSpanFromContext(previous);
      Span newSpan = safeSpanFromContext(next);
      if (oldSpan != newSpan && !oldSpan.getSpanContext().equals(newSpan.getSpanContext())) {
        updateThreadCorrelationStorage(newSpan);
      }
    } catch (Throwable t) {
      log.log(Level.SEVERE, "Error on context update", t);
    }
  }

  private volatile int writeForMemoryBarrier = 0;

  /**
   * This method ensures that all writes which happened prior to this method call
   * are not moved after the method call due to reordering.
   * <p>
   * This is realized based on the Java Memory Model guarantess for volatile variables.
   * Relevant resources:
   * <ul>
   *   <li><a href="https://stackoverflow.com/questions/17108541/happens-before-relationships-with-volatile-fields-and-synchronized-blocks-in-jav">StackOverflow topic</a></li>
   *   <li><a href="https://gee.cs.oswego.edu/dl/jmm/cookbook.html">JSR Compiler Cookbook</a></li>
   * </ul>
   */
  private void memoryStoreStoreBarrier() {
    writeForMemoryBarrier = 42;
  }

  private void updateThreadCorrelationStorage(Span newSpan) {
    ByteBuffer tls = UniversalProfilingCorrelation.getCurrentThreadStorage(true, TLS_STORAGE_SIZE);
    // tls might be null if unsupported or something went wrong on initialization
    if (tls != null) {
      // the valid flag is used to signal the host-agent that it is reading incomplete data
      tls.put(TLS_VALID_OFFSET, (byte) 0);
      memoryStoreStoreBarrier();
      tls.putChar(TLS_MINOR_VERSION_OFFSET, (char) 1);

      SpanContext spanCtx = newSpan.getSpanContext();
      if (spanCtx.isValid() && !spanCtx.isRemote()) {
        tls.put(TLS_TRACE_PRESENT_OFFSET, (byte) 1);
        tls.put(TLS_TRACE_FLAGS_OFFSET, spanCtx.getTraceFlags().asByte());
        HexUtils.writeHexAsBinary(spanCtx.getTraceId(), 0, tls, TLS_TRACE_ID_OFFSET, 16);
        HexUtils.writeHexAsBinary(spanCtx.getSpanId(), 0, tls, TLS_SPAN_ID_OFFSET, 8);
        // TODO: write local root span ID here
        HexUtils.writeHexAsBinary("0000000000000000", 0, tls, TLS_LOCAL_ROOT_SPAN_ID_OFFSET, 8);
      } else {
        tls.put(TLS_TRACE_PRESENT_OFFSET, (byte) 0);
      }
      memoryStoreStoreBarrier();
      tls.put(TLS_VALID_OFFSET, (byte) 1);
    }
  }

  private Span safeSpanFromContext(@Nullable Context context) {
    if (context == null) {
      return Span.getInvalid();
    }
    return Span.fromContext(context);
  }

  @Override
  public CompletableResultCode shutdown() {
    ActivationListener.setProcessor(null);
    UniversalProfilingCorrelation.reset();
    anyInstanceActive = false;
    return CompletableResultCode.ofSuccess();
  }

  private static class ActivationListener implements ContextStorage {

    static {
      // Ensures that this wrapper is registered EXACTLY once
      ContextStorage.addWrapper(ActivationListener::new);
    }

    @Nullable private static volatile UniversalProfilingProcessor processor = null;

    private final ContextStorage delegate;

    private ActivationListener(ContextStorage delegate) {
      this.delegate = delegate;
    }

    private static synchronized void setProcessor(
        @Nullable UniversalProfilingProcessor newProcessor) {
      if (newProcessor != null && processor != null) {
        throw new IllegalStateException("Only one processor can be registered at a time");
      }
      processor = newProcessor;
    }

    @Override
    public Scope attach(Context context) {
      UniversalProfilingProcessor currentProcessor = processor;
      if (currentProcessor != null) {
        Context previous = delegate.current();
        currentProcessor.onContextChange(previous, context);
        Scope delegateScope = delegate.attach(context);
        return () -> {
          UniversalProfilingProcessor proc = processor;
          if (proc != null) {
            proc.onContextChange(context, previous);
          }
          delegateScope.close();
        };
      } else {
        return delegate.attach(context);
      }
    }

    @Nullable
    @Override
    public Context current() {
      return delegate.current();
    }
  }
}
