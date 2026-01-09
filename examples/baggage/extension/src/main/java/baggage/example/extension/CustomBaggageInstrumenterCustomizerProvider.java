package baggage.example.extension;

import io.opentelemetry.api.baggage.Baggage;
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
        // then copy value in baggage
        String httpRoute = startAttributes.get(HttpAttributes.HTTP_ROUTE);

        if(httpRoute == null) {
          return context;
        }
        return Baggage.current()
            .toBuilder()
            .put("example.gateway.http.route", httpRoute)
            .build()
            .storeInContext(context);
      });
    }
  }
}
