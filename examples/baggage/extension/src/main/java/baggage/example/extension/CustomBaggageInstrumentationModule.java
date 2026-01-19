package baggage.example.extension;

import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import java.util.Collections;
import java.util.List;

public class CustomBaggageInstrumentationModule extends InstrumentationModule implements
    ExperimentalInstrumentationModule {

  public CustomBaggageInstrumentationModule() {
    super("custom-baggage");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new CustomBaggageInstrumentation());
  }

}
