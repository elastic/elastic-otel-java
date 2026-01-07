package baggage.example.extension;

import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizerProvider;
import io.opentelemetry.semconv.HttpAttributes;

import static io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer.InstrumentationType.HTTP_SERVER;

public class CustomBaggageInstrumenterCustomizerProvider implements InstrumenterCustomizerProvider {

  @Override
  public void customize(InstrumenterCustomizer customizer) {
    if (customizer.hasType(HTTP_SERVER)) {
      customizer.addContextCustomizer((context, o, startAttributes) -> {
        // retrieve HTTP route semantic convention attribute value when span is started,
        // then copy value into current context so it can be looked up later
        String httpRoute = startAttributes.get(HttpAttributes.HTTP_ROUTE);
        return httpRoute == null ? context : context.with(CustomBaggageSingletons.httpRouteContextKey(), httpRoute);
      });
    }
  }
}
