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
package co.elastic.otel.openai.v0_14;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import co.elastic.otel.openai.wrappers.InstrumentedOpenAiClient;
import com.openai.client.OpenAIClient;
import com.openai.core.ClientOptions;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class OpenAiOkHttpClientBuilderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.openai.client.okhttp.OpenAIOkHttpClient$Builder");
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        named("build").and(returns(named("com.openai.client.OpenAIClient"))),
        getClass().getName() + "$AdviceClass");
  }

  public static class AdviceClass {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    @Advice.AssignReturned.ToReturned
    public static OpenAIClient onExit(
        @Advice.Return OpenAIClient result,
        @Advice.FieldValue("clientOptions") ClientOptions.Builder clientOptions) {
      return InstrumentedOpenAiClient.wrap(result).baseUrl(clientOptions.baseUrl()).build();
    }
  }
}
