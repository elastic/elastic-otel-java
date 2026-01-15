package co.elastic.otel.sampling;

import static io.opentelemetry.sdk.trace.samplers.SamplingDecision.DROP;
import static io.opentelemetry.sdk.trace.samplers.SamplingDecision.RECORD_AND_SAMPLE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ElasticSamplerTest {

  // those tests rely on the implementation, but it's the simplest way to verify the effective
  // sampler configuration we get as multiple samplers are composed.

  @Test
  void defaultProbability() {
    Sampler sampler = new ElasticSampler.Builder().build();
    assertThat(sampler.getDescription()).isEqualTo("ComposableTraceIdRatioBasedSampler{threshold=0, ratio=1.0}");
  }

  @Test
  void highProbability() {
    Sampler sampler = new ElasticSampler.Builder().withProbability(0.99999999).build();
    assertThat(sampler.getDescription()).isEqualTo("ComposableTraceIdRatioBasedSampler{threshold=0000002af31dc8, ratio=0.99999999}");
  }

  @Test
  void halfProbability() {
    Sampler sampler = new ElasticSampler.Builder().withProbability(0.5).build();
    assertThat(sampler.getDescription()).isEqualTo("ComposableTraceIdRatioBasedSampler{threshold=8, ratio=0.5}");
  }

  @Test
  void offProbability() {
    Sampler sampler = new ElasticSampler.Builder().withProbability(0.0).build();
    assertThat(sampler.getDescription()).isEqualTo("ComposableTraceIdRatioBasedSampler{threshold=max, ratio=0.0}");
  }

  @Test
  void ignoreUrlPath() {
    Sampler sampler = new ElasticSampler.Builder().withIgnoredUrlPatterns(Collections.singletonList("/health/*")).build();
    checkSampling(sampler, Attributes.empty(), RECORD_AND_SAMPLE);
    checkSampling(sampler, Attributes.of(URL_PATH, "/health/"), DROP);
    checkSampling(sampler, Attributes.of(URL_PATH, "/health/test"), DROP);
    checkSampling(sampler, Attributes.of(URL_PATH, "/healthcheck"), RECORD_AND_SAMPLE);
  }

  @Test
  void ignoreUserAgent() {
    Sampler sampler = new ElasticSampler.Builder().withIgnoredUserAgentPatterns(
        Arrays.asList("curl*", "*Curly")).build();
    checkSampling(sampler, Attributes.empty(), RECORD_AND_SAMPLE);
    checkSampling(sampler, Attributes.of(USER_AGENT_ORIGINAL, "curl"), DROP);
    checkSampling(sampler, Attributes.of(USER_AGENT_ORIGINAL, "HappyCurly"), DROP);
    checkSampling(sampler, Attributes.of(USER_AGENT_ORIGINAL, "CURL"), RECORD_AND_SAMPLE);
  }

  private static void checkSampling(Sampler sampler, Attributes attributes,
      SamplingDecision expectedDecision) {
    SamplingResult samplingResult = sampler.shouldSample(Context.root(), TraceId.getInvalid(), "name", null,
        attributes,
        Collections.emptyList());
    assertThat(samplingResult.getDecision()).isEqualTo(expectedDecision);
  }

  @Test
  void singleGlobalBuilderInstance() {
    ElasticSampler.Builder first = ElasticSampler.INSTANCE.toBuilder();
    ElasticSampler.Builder second = ElasticSampler.INSTANCE.toBuilder();
    assertThat(first).isSameAs(second);
  }

}
