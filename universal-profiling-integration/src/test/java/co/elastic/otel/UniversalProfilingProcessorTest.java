package co.elastic.otel;

import static co.elastic.otel.UniversalProfilingProcessor.TLS_STORAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.ResourceAttributes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class UniversalProfilingProcessorTest {

  //TODO: add a test verifying that a context containing only a remote span is ignored
  @Test
  public void testNestedActivations() {
    try (OpenTelemetrySdk sdk = initSdk()) {

      Tracer tracer = sdk.getTracer("test-tracer");

      Span first = tracer.spanBuilder("first").startSpan();
      Span second = tracer.spanBuilder("second").startSpan();
      Span third = tracer.spanBuilder("third")
          .setParent(Context.current().with(second))
          .startSpan();

      checkTlsIs(Span.getInvalid());
      try (Scope s1 = first.makeCurrent()) {
        checkTlsIs(first);
        try (Scope nested = first.makeCurrent()) {
          checkTlsIs(first);
        }
        try (Scope s2 = second.makeCurrent()) {
          checkTlsIs(second);
          try (Scope s3 = third.makeCurrent()) {
            checkTlsIs(third);
            try (Scope reactivation = first.makeCurrent()) {
              checkTlsIs(first);
            }
            checkTlsIs(third);
          }
          checkTlsIs(second);
        }
        checkTlsIs(first);
      }
      checkTlsIs(Span.getInvalid());
    }
  }


  @Test
  public void testProcessStoragePopulated() {
    Resource withNamespace = Resource.builder()
        .put(ResourceAttributes.SERVICE_NAME, "service Ä 1")
        .put(ResourceAttributes.SERVICE_NAMESPACE, "my nameßspace")
        .build();
    try (OpenTelemetrySdk sdk = initSdk(withNamespace)) {
      checkProcessStorage("service Ä 1", "my nameßspace");
    }

    Resource withoutNamespace = Resource.builder()
        .put(ResourceAttributes.SERVICE_NAME, "service Ä 2")
        .build();
    try (OpenTelemetrySdk sdk = initSdk(withoutNamespace)) {
      checkProcessStorage("service Ä 2", "");
    }
  }

  private void checkProcessStorage(String expectedServiceName, String expectedEnv) {
    ByteBuffer buffer = JvmtiAccessImpl.createProcessProfilingCorrelationBufferAlias(1000);
    buffer.order(ByteOrder.nativeOrder());

    assertThat(buffer.getChar()).isEqualTo((char) 1); //layout-minor-version
    assertThat(readUtf8Str(buffer)).isEqualTo(expectedServiceName);
    assertThat(readUtf8Str(buffer)).isEqualTo(expectedEnv);
    assertThat(readUtf8Str(buffer)).isEqualTo(""); //socket file path
  }

  private static String readUtf8Str(ByteBuffer buffer) {
    int serviceNameLen = buffer.getInt();
    byte[] serviceUtf8 = new byte[serviceNameLen];
    buffer.get(serviceUtf8);
    return new String(serviceUtf8, StandardCharsets.UTF_8);
  }

  private void checkTlsIs(Span span) {
    ByteBuffer tls = JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(TLS_STORAGE_SIZE);
    if (tls != null) {
      tls.order(ByteOrder.nativeOrder());
      assertThat(tls.getChar(0)).isEqualTo((char) 1); //layout-minor-version
      assertThat(tls.get(2)).isEqualTo((byte) 1); //valid byte
    }

    SpanContext ctx = span.getSpanContext();
    if (ctx.isValid()) {
      assertThat(tls).isNotNull();
      assertThat(tls.get(3)).isEqualTo((byte) 1); //trace-present-flag
      assertThat(tls.get(4)).isEqualTo(ctx.getTraceFlags().asByte()); //trace-flags

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
      assertThat(localRootSpanId).containsExactly(0, 0, 0, 0, 0, 0, 0, 0);
    } else if (tls != null) {
      assertThat(tls.get(3)).isEqualTo((byte) 0); //trace-present-flag
    }

  }

  private static OpenTelemetrySdk initSdk() {
    Resource res = Resource.builder()
        .put(ResourceAttributes.SERVICE_NAME, "my-service")
        .build();
    return initSdk(res);
  }

  private static OpenTelemetrySdk initSdk(Resource res) {
    return OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder()
            .addResource(res)
            .addSpanProcessor(new UniversalProfilingProcessor(res))
            .build())
        .build();
  }

}
