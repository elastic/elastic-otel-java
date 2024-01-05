package co.elastic.otel.common.processor;


import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import co.elastic.otel.common.testutils.AssertionCollector;
import co.elastic.otel.common.testutils.AutoConfigTestProperties;
import co.elastic.otel.common.testutils.AutoConfiguredDataCapture;
import co.elastic.otel.common.testutils.OtelReflectionUtils;
import com.google.auto.service.AutoService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ChainingSpanProcessorAutoConfigurationTest {
  @BeforeEach
  public void reset() {
    AutoConfigA.delegate = (a, b) -> {
    };
    AutoConfigB.delegate = (a, b) -> {
    };
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  public void noProcessorCreatedWithoutExporter() {
    ChainingSpanProcessorAutoConfiguration mockConfig = Mockito.mock(
        ChainingSpanProcessorAutoConfiguration.class);
    AutoConfigA.delegate = mockConfig;

    try (AutoConfigTestProperties props = new AutoConfigTestProperties()
        .put("otel.traces.exporter", "NONE")) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      Tracer tracer = otel.getTracer("dummy-tracer");

      tracer.spanBuilder("span").startSpan().end();

      verifyNoInteractions(mockConfig);
    }
  }


  @Test
  public void multipleExporterProcessors() {

    AtomicReference<AbstractSimpleChainingSpanProcessor> chainingProcessor = new AtomicReference<>();

    AutoConfigA.delegate = (props, registerer) ->
        registerer.register(next -> {
          chainingProcessor.set(new AbstractSimpleChainingSpanProcessor(next) {});
          return chainingProcessor.get();
        });

    try (AutoConfigTestProperties props = new AutoConfigTestProperties()
        .put("otel.traces.exporter", "logging,otlp")) {
      //Logging will use a SimpleSpanExporter, otlp a BatchSpanProcessor

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      assertThat(chainingProcessor.get()).isNotNull();

      List<SpanProcessor> spanProcessors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(spanProcessors).containsExactlyInAnyOrder(
          chainingProcessor.get(),
          SpanProcessor.composite() //NOOP-processor
      );

      SpanProcessor terminal = chainingProcessor.get().next;
      assertThat(terminal).isInstanceOf(MutableCompositeSpanProcessor.class);

      List<SpanProcessor> exportingProcessors = OtelReflectionUtils
          .flattenCompositeProcessors(((MutableCompositeSpanProcessor) terminal).composite);

      assertThat(exportingProcessors)
          .hasSize(2)
          .anySatisfy(proc -> assertThat(proc).isInstanceOf(BatchSpanProcessor.class))
          .anySatisfy(proc -> assertThat(proc).isInstanceOf(SimpleSpanProcessor.class));
    }
  }


  @Test
  public void verifyProcessorOrder() {

    AssertionCollector assertionCollector = new AssertionCollector();

    AtomicInteger startCountA = new AtomicInteger();
    AtomicInteger startCountB = new AtomicInteger();
    AtomicInteger startCountC = new AtomicInteger();

    AtomicInteger endCountA = new AtomicInteger();
    AtomicInteger endCountB = new AtomicInteger();
    AtomicInteger endCountC = new AtomicInteger();

    AutoConfigA.delegate = (props, registry) -> {
      registry.register(next -> new AbstractSimpleChainingSpanProcessor(next) {

        @Override
        protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {
          int cnt = startCountA.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(startCountB.get()).isEqualTo(cnt - 1);
            assertThat(startCountC.get()).isEqualTo(cnt - 1);
          });
        }

        @Override
        protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
          int cnt = endCountA.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(endCountB.get()).isEqualTo(cnt - 1);
            assertThat(endCountC.get()).isEqualTo(cnt - 1);
            assertThat(AutoConfiguredDataCapture.getSpans())
                .hasSize(cnt - 1);
          });
          return readableSpan;
        }
      }, ChainingSpanProcessorRegisterer.ORDER_FIRST);

      registry.register(next -> new AbstractSimpleChainingSpanProcessor(next) {
        @Override
        protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {
          int cnt = startCountC.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(startCountA.get()).isEqualTo(cnt);
            assertThat(startCountB.get()).isEqualTo(cnt);
          });
        }

        @Override
        protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
          int cnt = endCountC.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(endCountA.get()).isEqualTo(cnt);
            assertThat(endCountB.get()).isEqualTo(cnt);
            assertThat(AutoConfiguredDataCapture.getSpans())
                .hasSize(cnt - 1);
          });
          return readableSpan;
        }
      }, ChainingSpanProcessorRegisterer.ORDER_LAST);
    };

    AutoConfigB.delegate = (props, registry) -> {
      registry.register(next -> new AbstractSimpleChainingSpanProcessor(next) {
        @Override
        protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {
          int cnt = startCountB.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(startCountA.get()).isEqualTo(cnt);
            assertThat(startCountC.get()).isEqualTo(cnt - 1);
          });
        }

        @Override
        protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
          int cnt = endCountB.incrementAndGet();
          assertionCollector.collect(() -> {
            assertThat(endCountA.get()).isEqualTo(cnt);
            assertThat(endCountC.get()).isEqualTo(cnt - 1);
            assertThat(AutoConfiguredDataCapture.getSpans())
                .hasSize(cnt - 1);
          });
          return readableSpan;
        }
      }, ChainingSpanProcessorRegisterer.ORDER_DEFAULT);

    };

    try (AutoConfigTestProperties props = new AutoConfigTestProperties()) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      Tracer tracer = otel.getTracer("dummy-tracer");

      Span first = tracer.spanBuilder("span-A").startSpan();
      assertionCollector.rethrowFirst();

      Span second = tracer.spanBuilder("span-B").startSpan();
      assertionCollector.rethrowFirst();

      assertThat(AutoConfiguredDataCapture.getSpans()).isEmpty();
      assertThat(startCountA.get()).isEqualTo(2);
      assertThat(startCountB.get()).isEqualTo(2);
      assertThat(startCountC.get()).isEqualTo(2);

      first.end();
      assertionCollector.rethrowFirst();
      assertThat(AutoConfiguredDataCapture.getSpans())
          .hasSize(1)
          .element(0)
          .satisfies(span -> assertThat(span).hasName("span-A"));

      second.end();
      assertionCollector.rethrowFirst();
      assertThat(AutoConfiguredDataCapture.getSpans())
          .hasSize(2)
          .element(1)
          .satisfies(span -> assertThat(span).hasName("span-B"));
      assertThat(endCountA.get()).isEqualTo(2);
      assertThat(endCountB.get()).isEqualTo(2);
      assertThat(endCountC.get()).isEqualTo(2);
    }
  }

  @AutoService(ChainingSpanProcessorAutoConfiguration.class)
  public static class AutoConfigA implements ChainingSpanProcessorAutoConfiguration {
    public static ChainingSpanProcessorAutoConfiguration delegate = (a, b) -> {
    };

    @Override
    public void registerSpanProcessors(ConfigProperties properties,
        ChainingSpanProcessorRegisterer registerer) {
      delegate.registerSpanProcessors(properties, registerer);
    }
  }


  @AutoService(ChainingSpanProcessorAutoConfiguration.class)
  public static class AutoConfigB implements ChainingSpanProcessorAutoConfiguration {
    public static ChainingSpanProcessorAutoConfiguration delegate = (a, b) -> {
    };

    @Override
    public void registerSpanProcessors(ConfigProperties properties,
        ChainingSpanProcessorRegisterer registerer) {
      delegate.registerSpanProcessors(properties, registerer);
    }
  }

}
