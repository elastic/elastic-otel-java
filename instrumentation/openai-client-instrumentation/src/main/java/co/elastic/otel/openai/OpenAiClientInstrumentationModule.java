package co.elastic.otel.openai;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class OpenAiClientInstrumentationModule extends InstrumentationModule {

  public OpenAiClientInstrumentationModule() {
    super("openai-client");
  }

  @Override
  public boolean defaultEnabled(ConfigProperties config) {
    return false;
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
