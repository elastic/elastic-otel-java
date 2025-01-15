package co.elastic.otel.openai;

import static com.github.tomakehurst.wiremock.client.WireMock.proxyAllTo;
import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

final class OpenAIRecordingExtension extends WireMockExtension {

  OpenAIClient client;

  private final String testName;

  OpenAIRecordingExtension(String testName) {
    super(WireMockExtension.newInstance()
        .options(options().extensions(ResponseHeaderScrubber.class,
                PrettyPrintEqualToJsonStubMappingTransformer.class)
            .mappingSource(new YamlFileMappingsSource(
                new SingleRootFileSource("src/test/resources").child("mappings")))));
    this.testName = testName;
  }

  @Override
  protected void onBeforeEach(WireMockRuntimeInfo wireMock) {
    YamlFileMappingsSource.setCurrentTest(testName);
    client = OpenAIOkHttpClient.builder()
        .apiKey(System.getenv().getOrDefault("OPENAI_API_KEY", "testing"))
        .baseUrl("http://localhost:" + wireMock.getHttpPort())
        .build();

    // Set a low priority so recordings are used when available
    String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
    stubFor(proxyAllTo(baseUrl)
        .atPriority(Integer.MAX_VALUE));
    startRecording(recordSpec().forTarget(baseUrl)
        .transformers("scrub-response-header", "pretty-print-equal-to-json")
        // Include all bodies inline.
        .extractTextBodiesOver(Long.MAX_VALUE)
        .extractBinaryBodiesOver(Long.MAX_VALUE));
  }

  @Override
  protected void onAfterEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
    YamlFileMappingsSource.setCurrentTest(testName);
    stopRecording();
  }

}
