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
package co.elastic.otel.openai.wrappers;

import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_OPERATION_NAME;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_REQUEST_ENCODING_FORMATS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_REQUEST_MODEL;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_RESPONSE_MODEL;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_SYSTEM;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_USAGE_INPUT_TOKENS;

import com.openai.core.RequestOptions;
import com.openai.models.CreateEmbeddingResponse;
import com.openai.models.EmbeddingCreateParams;
import com.openai.services.blocking.EmbeddingService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class InstrumentedEmbeddingsService implements EmbeddingService {

  private final EmbeddingService delegate;
  private final InstrumentationSettings settings;

  InstrumentedEmbeddingsService(EmbeddingService delegate, InstrumentationSettings settings) {
    this.delegate = delegate;
    this.settings = settings;
  }

  private static class RequestHolder {
    final EmbeddingCreateParams request;
    final InstrumentationSettings settings;

    public RequestHolder(EmbeddingCreateParams request, InstrumentationSettings settings) {
      this.request = request;
      this.settings = settings;
    }
  }

  public static final Instrumenter<RequestHolder, CreateEmbeddingResponse> INSTRUMENTER =
      Instrumenter.<RequestHolder, CreateEmbeddingResponse>builder(
              GlobalOpenTelemetry.get(),
              Constants.INSTRUMENTATION_NAME,
              req -> "embeddings " + req.request.model())
          .addAttributesExtractor(
              new AttributesExtractor<RequestHolder, CreateEmbeddingResponse>() {

                @Override
                public void onStart(
                    AttributesBuilder attributes,
                    Context parentContext,
                    RequestHolder requestHolder) {
                  EmbeddingCreateParams request = requestHolder.request;
                  requestHolder.settings.putServerInfoIntoAttributes(attributes);
                  attributes
                      .put(GEN_AI_SYSTEM, "openai")
                      .put(GEN_AI_OPERATION_NAME, "embeddings")
                      .put(GEN_AI_REQUEST_MODEL, request.model().toString());
                  request
                      .encodingFormat()
                      .ifPresent(
                          format ->
                              attributes.put(
                                  GEN_AI_REQUEST_ENCODING_FORMATS,
                                  Collections.singletonList(format.toString())));
                }

                @Override
                public void onEnd(
                    AttributesBuilder attributes,
                    Context context,
                    RequestHolder requestHolder,
                    CreateEmbeddingResponse embeddings,
                    Throwable error) {
                  if (embeddings != null) {
                    attributes.put(GEN_AI_USAGE_INPUT_TOKENS, embeddings.usage().promptTokens());
                    attributes.put(GEN_AI_RESPONSE_MODEL, embeddings.model());
                  }
                }
              })
          .buildInstrumenter(SpanKindExtractor.alwaysClient());

  @NotNull
  @Override
  public CreateEmbeddingResponse create(
      @NotNull EmbeddingCreateParams embeddingCreateParams,
      @NotNull RequestOptions requestOptions) {
    RequestHolder requestHolder = new RequestHolder(embeddingCreateParams, settings);

    Context parentCtx = Context.current();
    if (!INSTRUMENTER.shouldStart(parentCtx, requestHolder)) {
      return delegate.create(embeddingCreateParams, requestOptions);
    }

    Context ctx = INSTRUMENTER.start(parentCtx, requestHolder);
    CreateEmbeddingResponse response;
    try (Scope scope = ctx.makeCurrent()) {
      response = delegate.create(embeddingCreateParams, requestOptions);
    } catch (Throwable t) {
      INSTRUMENTER.end(ctx, requestHolder, null, t);
      throw t;
    }

    INSTRUMENTER.end(ctx, requestHolder, response, null);
    return response;
  }
}
