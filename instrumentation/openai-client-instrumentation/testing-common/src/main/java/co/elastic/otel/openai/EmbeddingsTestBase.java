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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_ENCODING_FORMATS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_SYSTEM;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openai.errors.NotFoundException;
import com.openai.models.CreateEmbeddingResponse;
import com.openai.models.EmbeddingCreateParams;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddingsTestBase {
  private static final String MODEL =
      System.getenv().getOrDefault("OPENAI_MODEL", "text-embedding-3-small");

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension
  static final OpenAIRecordingExtension openai = new OpenAIRecordingExtension("EmbeddingsTest");

  @Test
  void basic() {
    String text = "South Atlantic Ocean.";

    EmbeddingCreateParams request =
        EmbeddingCreateParams.builder()
            .model(MODEL)
            .encodingFormat(EmbeddingCreateParams.EncodingFormat.BASE64)
            .inputOfArrayOfStrings(Collections.singletonList(text))
            .build();
    CreateEmbeddingResponse response = openai.client.embeddings().create(request);

    assertThat(response.data()).hasSize(1);

    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("embeddings " + MODEL)
                        .hasKind(SpanKind.CLIENT)
                        .hasStatusSatisfying(status -> status.hasCode(StatusCode.UNSET))
                        .hasAttributesSatisfying(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "embeddings"),
                            equalTo(GEN_AI_REQUEST_MODEL, MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, "text-embedding-3-small"),
                            equalTo(
                                GEN_AI_REQUEST_ENCODING_FORMATS,
                                Collections.singletonList("base64")),
                            equalTo(GEN_AI_USAGE_INPUT_TOKENS, 4),
                            equalTo(SERVER_ADDRESS, "localhost"),
                            equalTo(SERVER_PORT, (long) openai.getPort()))));
  }

  @Test
  void invalidModel() {
    String text = "South Atlantic Ocean.";

    EmbeddingCreateParams request =
        EmbeddingCreateParams.builder()
            .model("not-a-model")
            .encodingFormat(EmbeddingCreateParams.EncodingFormat.BASE64)
            .inputOfArrayOfStrings(Collections.singletonList(text))
            .build();

    assertThatThrownBy(() -> openai.client.embeddings().create(request))
        .isInstanceOf(NotFoundException.class);

    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("embeddings not-a-model")
                        .hasKind(SpanKind.CLIENT)
                        .hasStatusSatisfying(status -> status.hasCode(StatusCode.ERROR))
                        .hasAttributesSatisfying(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "embeddings"),
                            equalTo(GEN_AI_REQUEST_MODEL, "not-a-model"),
                            equalTo(
                                GEN_AI_REQUEST_ENCODING_FORMATS,
                                Collections.singletonList("base64")),
                            equalTo(SERVER_ADDRESS, "localhost"),
                            equalTo(SERVER_PORT, (long) openai.getPort()))));
  }
}
