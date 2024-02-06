import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.elastic.otel.common.ElasticAttributes;
import co.elastic.otel.testing.DisabledOnAppleSilicon;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

@DisabledOnOs(OS.WINDOWS)
@DisabledOnAppleSilicon
public class InferredSpansTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  public void checkInferredSpansFunctional() {
    rootSpan();
    await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
      List<SpanData> spans = testing.spans();
      assertThat(spans).hasSize(2);

      assertThat(spans).anySatisfy(span -> assertThat(span).hasName("InferredSpansTest.rootSpan"));

      SpanData parent = spans.stream()
          .filter(span -> span.getName().equals("InferredSpansTest.rootSpan"))
          .findFirst().get();

      assertThat(spans).anySatisfy(span -> assertThat(span)
          .hasName("InferredSpansTest#rootSpan")
          .hasParent(parent)
          .hasAttribute(ElasticAttributes.IS_INFERRED, true)
      );
    });
  }

  @WithSpan
  public void rootSpan() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
