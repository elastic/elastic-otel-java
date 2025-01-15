package co.elastic.otel.openai;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.List;

@AutoService(InstrumentationModule.class)
public class OpenAiClientInstrumentationModule extends InstrumentationModule {

    public OpenAiClientInstrumentationModule() {
        super("openai-client");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return List.of(new OpenAiOkHttpClientBuilderInstrumentation());
    }

    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("co.elastic.otel.openai");
    }
}
