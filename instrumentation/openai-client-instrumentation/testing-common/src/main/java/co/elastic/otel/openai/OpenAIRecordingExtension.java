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

  /**
   * Setting this to true will make the tests call the real OpenAI API instead and record the
   * responses. You'll have to setup the OPENAI_API_KEY variable for this to work.
   */
  private static final boolean RECORD_WITH_REAL_API = false;

  OpenAIClient client;

  private final String testName;

  OpenAIRecordingExtension(String testName) {
    super(
        WireMockExtension.newInstance()
            .options(
                options()
                    .extensions(
                        ResponseHeaderScrubber.class,
                        PrettyPrintEqualToJsonStubMappingTransformer.class)
                    .mappingSource(
                        new YamlFileMappingsSource(
                            new SingleRootFileSource("../testing-common/src/main/resources")
                                .child("mappings")))));
    this.testName = testName;
  }

  @Override
  protected void onBeforeEach(WireMockRuntimeInfo wireMock) {
    YamlFileMappingsSource.setCurrentTest(testName);
    client =
        OpenAIOkHttpClient.builder()
            .apiKey(System.getenv().getOrDefault("OPENAI_API_KEY", "testing"))
            .baseUrl("http://localhost:" + wireMock.getHttpPort())
            .build();

    // Set a low priority so recordings are used when available
    if (RECORD_WITH_REAL_API) {
      String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
      stubFor(proxyAllTo(baseUrl).atPriority(Integer.MAX_VALUE));
      startRecording(
          recordSpec()
              .forTarget(baseUrl)
              .transformers("scrub-response-header", "pretty-print-equal-to-json")
              // Include all bodies inline.
              .extractTextBodiesOver(Long.MAX_VALUE)
              .extractBinaryBodiesOver(Long.MAX_VALUE));
    }
  }

  @Override
  protected void onAfterEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
    if (RECORD_WITH_REAL_API) {
      YamlFileMappingsSource.setCurrentTest(testName);
      stopRecording();
    }
  }
}
