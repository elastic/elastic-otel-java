package baggage.example.extension;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

public class FilteringBaggagePropagatorProvider implements ConfigurablePropagatorProvider {

  @Override
  public TextMapPropagator getPropagator(ConfigProperties configProperties) {
    return new FilteringBaggagePropagator(W3CBaggagePropagator.getInstance());
  }

  @Override
  public String getName() {
    return "baggage-filtering";
  }
}
