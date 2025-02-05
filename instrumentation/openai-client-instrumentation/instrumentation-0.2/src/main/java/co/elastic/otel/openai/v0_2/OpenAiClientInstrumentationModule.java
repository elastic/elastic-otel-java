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
package co.elastic.otel.openai.v0_2;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.not;

import co.elastic.otel.openai.wrappers.Constants;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class OpenAiClientInstrumentationModule extends InstrumentationModule {

  public OpenAiClientInstrumentationModule() {
    super(Constants.INSTRUMENTATION_NAME);
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // HandlerReferencingAsyncStreamResponse was added in 0.14.1,
    // which is the next release after 0.13.0
    // 0.14.0 was a broken release which doesn't exist on maven central
    return not(hasClassesNamed("com.openai.core.http.HandlerReferencingAsyncStreamResponse"));
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new OpenAiOkHttpClientBuilderInstrumentation());
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("co.elastic.otel.openai");
  }
}
