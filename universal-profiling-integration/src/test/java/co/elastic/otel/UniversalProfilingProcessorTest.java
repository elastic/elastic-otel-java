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

import static co.elastic.otel.UniversalProfilingProcessor.TLS_STORAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.otel.testing.MapGetter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.ResourceAttributes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.WINDOWS)
public class UniversalProfilingProcessorTest {

  @Test
  public void testRemoteSpanIgnored() {
    try (OpenTelemetrySdk sdk = initSdk()) {

      Map<String, String> headers = new HashMap<>();
      headers.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01");
      Context remoteCtx =
          W3CTraceContextPropagator.getInstance().extract(Context.root(), headers, new MapGetter());

      assertThat(Span.fromContext(remoteCtx).getSpanContext().isRemote()).isTrue();

      Tracer tracer = sdk.getTracer("test-tracer");

      Span span = tracer.spanBuilder("first").startSpan();

      checkTlsIs(Span.getInvalid(), null);
      try (Scope s1 = span.makeCurrent()) {
        checkTlsIs(span, span);
        try (Scope s2 = remoteCtx.makeCurrent()) {
          checkTlsIs(Span.getInvalid(), null);
        }
        checkTlsIs(span, span);
      }
    }
  }

  @Test
  public void testNestedActivations() {
    try (OpenTelemetrySdk sdk = initSdk()) {

      Tracer tracer = sdk.getTracer("test-tracer");

      Span first = tracer.spanBuilder("first").startSpan();
      Span second = tracer.spanBuilder("second").startSpan();
      Span third =
          tracer.spanBuilder("third").setParent(Context.current().with(second)).startSpan();

      checkTlsIs(Span.getInvalid(), null);
      try (Scope s1 = first.makeCurrent()) {
        checkTlsIs(first, first);
        try (Scope nested = first.makeCurrent()) {
          checkTlsIs(first, first);
        }
        try (Scope s2 = second.makeCurrent()) {
          checkTlsIs(second, second);
          try (Scope s3 = third.makeCurrent()) {
            checkTlsIs(third, second);
            try (Scope reactivation = first.makeCurrent()) {
              checkTlsIs(first, first);
            }
            checkTlsIs(third, second);
          }
          checkTlsIs(second, second);
        }
        checkTlsIs(first, first);
      }
      checkTlsIs(Span.getInvalid(), null);
    }
  }

  @Test
  public void testProcessStoragePopulated() {
    Resource withNamespace =
        Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, "service Ä 1")
            .put(ResourceAttributes.SERVICE_NAMESPACE, "my nameßspace")
            .build();
    try (OpenTelemetrySdk sdk = initSdk(withNamespace)) {
      checkProcessStorage("service Ä 1", "my nameßspace");
    }

    Resource withoutNamespace =
        Resource.builder().put(ResourceAttributes.SERVICE_NAME, "service Ä 2").build();
    try (OpenTelemetrySdk sdk = initSdk(withoutNamespace)) {
      checkProcessStorage("service Ä 2", "");
    }
  }

  private void checkProcessStorage(String expectedServiceName, String expectedEnv) {
    ByteBuffer buffer = JvmtiAccessImpl.createProcessProfilingCorrelationBufferAlias(1000);
    buffer.order(ByteOrder.nativeOrder());

    assertThat(buffer.getChar()).isEqualTo((char) 1); // layout-minor-version
    assertThat(readUtf8Str(buffer)).isEqualTo(expectedServiceName);
    assertThat(readUtf8Str(buffer)).isEqualTo(expectedEnv);
    assertThat(readUtf8Str(buffer)).isEqualTo(""); // socket file path
  }

  private static String readUtf8Str(ByteBuffer buffer) {
    int serviceNameLen = buffer.getInt();
    byte[] serviceUtf8 = new byte[serviceNameLen];
    buffer.get(serviceUtf8);
    return new String(serviceUtf8, StandardCharsets.UTF_8);
  }

  private void checkTlsIs(Span span, Span localRoot) {
    ByteBuffer tls = JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(TLS_STORAGE_SIZE);
    if (tls != null) {
      tls.order(ByteOrder.nativeOrder());
      assertThat(tls.getChar(0)).isEqualTo((char) 1); // layout-minor-version
      assertThat(tls.get(2)).isEqualTo((byte) 1); // valid byte
    }

    SpanContext ctx = span.getSpanContext();
    if (ctx.isValid()) {
      assertThat(tls).isNotNull();
      assertThat(tls.get(3)).isEqualTo((byte) 1); // trace-present-flag
      assertThat(tls.get(4)).isEqualTo(ctx.getTraceFlags().asByte()); // trace-flags

      byte[] traceId = new byte[16];
      byte[] spanId = new byte[8];
      byte[] localRootSpanId = new byte[8];
      tls.position(5);
      tls.get(traceId);
      assertThat(traceId).containsExactly(ctx.getTraceIdBytes());
      tls.position(21);
      tls.get(spanId);
      assertThat(spanId).containsExactly(ctx.getSpanIdBytes());
      tls.position(29);
      tls.get(localRootSpanId);
      assertThat(localRootSpanId).containsExactly(localRoot.getSpanContext().getSpanIdBytes());
    } else if (tls != null) {
      assertThat(tls.get(3)).isEqualTo((byte) 0); // trace-present-flag
    }
  }

  private static OpenTelemetrySdk initSdk() {
    Resource res = Resource.builder().put(ResourceAttributes.SERVICE_NAME, "my-service").build();
    return initSdk(res);
  }

  private static OpenTelemetrySdk initSdk(Resource res) {
    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addResource(res)
                .addSpanProcessor(new UniversalProfilingProcessor(res))
                .build())
        .build();
  }
}
