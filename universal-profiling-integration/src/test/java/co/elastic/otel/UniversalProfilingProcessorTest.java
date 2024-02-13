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

import static co.elastic.otel.ProfilerSharedMemoryWriter.TLS_STORAGE_SIZE;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import co.elastic.otel.common.ElasticAttributes;
import co.elastic.otel.testing.MapGetter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ResourceAttributes;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisabledOnOs(OS.WINDOWS)
public class UniversalProfilingProcessorTest {

  InMemorySpanExporter spans;
  UniversalProfilingProcessor processor;

  @TempDir private Path tempDir;

  @BeforeEach
  public void reset() {
    spans = null;
    processor = null;
  }

  private OpenTelemetrySdk initSdk() {
    return initSdk(builder -> {});
  }

  private OpenTelemetrySdk initSdk(Consumer<UniversalProfilingProcessorBuilder> customizer) {
    Resource res = Resource.builder().put(ResourceAttributes.SERVICE_NAME, "my-service").build();
    return initSdk(res, customizer, Sampler.alwaysOn());
  }

  private OpenTelemetrySdk initSdk(
      Consumer<UniversalProfilingProcessorBuilder> customizer, Sampler sampler) {
    Resource res = Resource.builder().put(ResourceAttributes.SERVICE_NAME, "my-service").build();
    return initSdk(res, customizer, sampler);
  }

  private OpenTelemetrySdk initSdk(
      Resource res, Consumer<UniversalProfilingProcessorBuilder> customizer, Sampler sampler) {
    spans = InMemorySpanExporter.create();

    // The InMemoryExporter resets the buffered spans on shutdown
    // we however want to assert the exported spans after shutdown in some tests
    SpanExporter ignoreShutdown =
        new SpanExporter() {
          @Override
          public CompletableResultCode export(Collection<SpanData> collection) {
            return spans.export(collection);
          }

          @Override
          public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
          }

          @Override
          public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
          }
        };

    SpanProcessor exporter =
        SimpleSpanProcessor.builder(ignoreShutdown).setExportUnsampledSpans(true).build();

    UniversalProfilingProcessorBuilder builder =
        UniversalProfilingProcessor.builder(exporter, res).socketDir(tempDir.toString());
    customizer.accept(builder);
    processor = builder.build();
    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addResource(res)
                .addSpanProcessor(processor)
                .setSampler(sampler)
                .build())
        .build();
  }

  @Nested
  class SharedMemory {

    @Test
    public void testRemoteSpanIgnored() {
      try (OpenTelemetrySdk sdk = initSdk()) {

        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01");
        Context remoteCtx =
            W3CTraceContextPropagator.getInstance()
                .extract(Context.root(), headers, new MapGetter());

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
      try (OpenTelemetrySdk sdk = initSdk(withNamespace, b -> {}, Sampler.alwaysOn())) {
        checkProcessStorage("service Ä 1", "my nameßspace");
      }

      Resource withoutNamespace =
          Resource.builder().put(ResourceAttributes.SERVICE_NAME, "service Ä 2").build();
      try (OpenTelemetrySdk sdk = initSdk(withoutNamespace, b -> {}, Sampler.alwaysOn())) {
        checkProcessStorage("service Ä 2", "");
      }
    }

    private void checkProcessStorage(String expectedServiceName, String expectedEnv) {
      ByteBuffer buffer = JvmtiAccessImpl.createProcessProfilingCorrelationBufferAlias(1000);
      buffer.order(ByteOrder.nativeOrder());

      String sockRegex = tempDir.toAbsolutePath().toString() + "/essock[a-zA-Z0-9]{8}";

      assertThat(buffer.getChar()).isEqualTo((char) 1); // layout-minor-version
      assertThat(readUtf8Str(buffer)).isEqualTo(expectedServiceName);
      assertThat(readUtf8Str(buffer)).isEqualTo(expectedEnv);
      assertThat(readUtf8Str(buffer)).matches(sockRegex); // socket file path
    }

    private static String readUtf8Str(ByteBuffer buffer) {
      int serviceNameLen = buffer.getInt();
      byte[] serviceUtf8 = new byte[serviceNameLen];
      buffer.get(serviceUtf8);
      return new String(serviceUtf8, StandardCharsets.UTF_8);
    }

    private void checkTlsIs(Span span, Span localRoot) {
      ByteBuffer tls =
          JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(TLS_STORAGE_SIZE);
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
  }

  @Nested
  class SpanCorrelation {

    @Test
    void checkCorrelationFunctional() {

      AtomicLong clock = new AtomicLong(0L);

      try (OpenTelemetrySdk sdk =
          initSdk(builder -> builder.clock(clock::get).spanDelay(Duration.ofNanos(1)))) {
        Tracer tracer = sdk.getTracer("test-tracer");

        Span span1 = tracer.spanBuilder("span1").startSpan();
        Span span2 = tracer.spanBuilder("span2").startSpan();

        byte[] st1 = randomStackTraceId(1);
        sendMsg(span1, st1, 1);

        // Send some garbage which should not affect our processing
        JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(new byte[] {1, 2, 3});

        byte[] st2 = randomStackTraceId(2);
        sendMsg(span2, st2, 2);

        // ensure that the messages are processed now
        processor.pollMessageAndFlushPendingSpans();

        span1.end();
        span2.end();

        // ensure that spans are not sent, their delay has not yet elapsed
        assertThat(spans.getFinishedSpanItems()).isEmpty();

        byte[] st3 = randomStackTraceId(3);
        sendMsg(span2, st2, 1);
        sendMsg(span1, st3, 2);
        sendMsg(span2, st3, 1);

        clock.set(1L);
        // now the background thread should consume those messages and flush the spans
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(
                () -> {
                  List<SpanData> spanData = spans.getFinishedSpanItems();
                  assertThat(spanData)
                      .hasSize(2)
                      .anySatisfy(sp -> assertThat(sp).hasName("span1"))
                      .anySatisfy(sp -> assertThat(sp).hasName("span2"));

                  SpanData sp1Data =
                      spanData.stream()
                          .filter(sp -> sp.getName().equals("span1"))
                          .findFirst()
                          .get();
                  assertThat(
                          sp1Data.getAttributes().get(ElasticAttributes.PROFILER_STACK_TRACE_IDS))
                      .containsExactlyInAnyOrder(base64(st1), base64(st3), base64(st3));

                  SpanData sp2Data =
                      spanData.stream()
                          .filter(sp -> sp.getName().equals("span2"))
                          .findFirst()
                          .get();
                  assertThat(
                          sp2Data.getAttributes().get(ElasticAttributes.PROFILER_STACK_TRACE_IDS))
                      .containsExactlyInAnyOrder(
                          base64(st2), base64(st2), base64(st2), base64(st3));
                });
      }
    }

    @Test
    void unsampledSpansNotCorrelated() {
      Sampler sampler =
          new Sampler() {
            @Override
            public SamplingResult shouldSample(
                Context context,
                String s,
                String s1,
                SpanKind spanKind,
                Attributes attributes,
                List<LinkData> list) {
              return SamplingResult.recordOnly();
            }

            @Override
            public String getDescription() {
              return "";
            }
          };
      try (OpenTelemetrySdk sdk =
          initSdk(builder -> builder.clock(() -> 0L).spanDelay(Duration.ofNanos(1)), sampler)) {
        Tracer tracer = sdk.getTracer("test-tracer");

        Span span1 = tracer.spanBuilder("span1").startSpan();
        assertThat(span1.getSpanContext().isSampled()).isFalse();

        // Still send a stacktrace to make sure it is actually ignored
        sendMsg(span1, randomStackTraceId(1), 1);

        // ensure that the messages are processed now
        processor.pollMessageAndFlushPendingSpans();
        span1.end();

        assertThat(spans.getFinishedSpanItems())
            .hasSize(1)
            .anySatisfy(
                sp -> {
                  assertThat(sp).hasName("span1");
                  assertThat(sp.getAttributes().get(ElasticAttributes.PROFILER_STACK_TRACE_IDS))
                      .isNull();
                });
      }
    }

    @Test
    void nonLocalRootSpansNotDelayed() {
      try (OpenTelemetrySdk sdk =
          initSdk(builder -> builder.clock(() -> 0L).spanDelay(Duration.ofNanos(1)))) {
        Tracer tracer = sdk.getTracer("test-tracer");

        Span root = tracer.spanBuilder("root").startSpan();
        Span child = tracer.spanBuilder("child").setParent(Context.root().with(root)).startSpan();

        // Still send a stacktrace to make sure it is actually ignored
        sendMsg(child, randomStackTraceId(1), 1);

        // ensure that the messages are processed now
        processor.pollMessageAndFlushPendingSpans();
        child.end();

        assertThat(spans.getFinishedSpanItems())
            .hasSize(1)
            .anySatisfy(
                sp -> {
                  assertThat(sp).hasName("child");
                  assertThat(sp.getAttributes().get(ElasticAttributes.PROFILER_STACK_TRACE_IDS))
                      .isNull();
                });
      }
    }

    @Test
    void shutdownFlushesBufferedSpans() {
      byte[] st1 = randomStackTraceId(1);

      try (OpenTelemetrySdk sdk =
          initSdk(builder -> builder.clock(() -> 0L).spanDelay(Duration.ofNanos(1)))) {
        Tracer tracer = sdk.getTracer("test-tracer");

        Span root = tracer.spanBuilder("root").startSpan();
        root.end();

        assertThat(spans.getFinishedSpanItems()).isEmpty();

        sendMsg(root, st1, 1);
      }

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(
              sp -> {
                assertThat(sp).hasName("root");
                assertThat(sp.getAttributes().get(ElasticAttributes.PROFILER_STACK_TRACE_IDS))
                    .containsExactly(base64(st1));
              });
    }

    @Test
    void bufferCapacityExceeded() {
      try (OpenTelemetrySdk sdk =
          initSdk(
              builder -> builder.clock(() -> 0L).spanDelay(Duration.ofNanos(1)).bufferSize(2))) {

        Tracer tracer = sdk.getTracer("test-tracer");

        Span span1 = tracer.spanBuilder("span1").startSpan();
        span1.end();
        Span span2 = tracer.spanBuilder("span2").startSpan();
        span2.end();
        // now the buffer should be full, span 3 should be sent immediately
        Span span3 = tracer.spanBuilder("span3").startSpan();
        span3.end();

        assertThat(spans.getFinishedSpanItems())
            .hasSize(1)
            .anySatisfy(sp -> assertThat(sp).hasName("span3"));
      }
    }

    @Test
    void badSocketPath() throws Exception {
      Path notADir = tempDir.resolve("not_a_dir");
      Files.createFile(notADir);
      String absPath = notADir.toAbsolutePath().toString();

      assertThatThrownBy(
              () -> {
                try (OpenTelemetrySdk sdk = initSdk(builder -> builder.socketDir(absPath))) {}
              })
          .hasMessageContaining("socket");

      // Ensure no garbage is left behind, we can cleanly start again with good settings
      try (OpenTelemetrySdk sdk = initSdk()) {
        assertThat(Paths.get(processor.socketPath)).exists();
      }
    }

    @Test
    void socketParentDirCreated() throws Exception {
      Path subDirs = tempDir.resolve("create/me");
      String absolute = subDirs.toAbsolutePath().toString();

      // Ensure no garbage is left behind, we can cleanly start with good settings
      try (OpenTelemetrySdk sdk = initSdk(builder -> builder.socketDir(absolute))) {
        assertThat(Paths.get(processor.socketPath)).exists();
        assertThat(processor.socketPath).startsWith(absolute + "/");
      }
    }

    @Test
    void ensureCorrelationDoesNotPreventSpanGC() {
      try (OpenTelemetrySdk sdk = initSdk()) {
        Tracer tracer = sdk.getTracer("dummy-scope");
        Span gcedWithoutEnd = tracer.spanBuilder("root").startSpan();

        WeakReference weakSpan = new WeakReference(gcedWithoutEnd);
        gcedWithoutEnd = null;

        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(1))
            .untilAsserted(
                () -> {
                  System.gc();
                  assertThat(weakSpan.get()).isNull();
                });
      }
    }

    private byte[] randomStackTraceId(int seed) {
      byte[] id = new byte[16];
      new Random(seed).nextBytes(id);
      return id;
    }

    private String base64(byte[] data) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    void sendMsg(Span localRoot, byte[] stackTraceId, int count) {
      byte[] traceId = localRoot.getSpanContext().getTraceIdBytes();
      byte[] rootSpanId = localRoot.getSpanContext().getSpanIdBytes();

      ByteBuffer message = ByteBuffer.allocate(46);
      message.order(ByteOrder.nativeOrder());
      message.putShort((short) 1); // message-type = correlation message
      message.putShort((short) 1); // message-version
      message.put(traceId);
      message.put(rootSpanId);
      message.put(stackTraceId);
      message.putShort((short) count);

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(message.array());
    }
  }
}
