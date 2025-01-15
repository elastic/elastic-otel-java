package co.elastic.otel.openai;

import co.elastic.otel.openai.wrappers.InstrumentedOpenAiClient;
import com.openai.client.OpenAIClient;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

public class OpenAiOkHttpClientBuilderInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.openai.client.okhttp.OpenAIOkHttpClient$Builder");
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
        typeTransformer.applyAdviceToMethod(
                named("build").and(returns(named("com.openai.client.OpenAIClient"))),
                "co.elastic.otel.openai.OpenAiOkHttpClientBuilderInstrumentation$AdviceClass"
        );
    }

    public static class AdviceClass {

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
        @Advice.AssignReturned.ToReturned
        public static OpenAIClient onExit(
                @Advice.Return OpenAIClient result,
                @Advice.FieldValue("baseUrl") String baseUrl
        ) {
            return InstrumentedOpenAiClient.wrap(result).baseUrl(baseUrl).build();
        }
    }
}
