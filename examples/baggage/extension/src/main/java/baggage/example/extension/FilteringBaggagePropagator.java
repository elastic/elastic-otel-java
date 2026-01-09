package baggage.example.extension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.semconv.ServerAttributes;
import java.util.Collection;

/**
 * A custom context propagator that allows to filter baggage propagation based on span attributes.
 * <br/>
 * Configuration is not automatically updated when this extension is used, users must set the {@code otel.propagators} configuration
 * to {@code tracecontext,baggage-filtering} to replace the {@code tracecontext,baggage} default value.
 */
public class FilteringBaggagePropagator implements TextMapPropagator {

  private final TextMapPropagator delegate;

  public FilteringBaggagePropagator(TextMapPropagator delegate) {
    this.delegate = delegate;
  }

  @Override
  public Collection<String> fields() {
    return delegate.fields();
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {

    boolean allow = false;

    Span span = Span.fromContext(context);
    if (span instanceof ReadableSpan) {
      ReadableSpan readableSpan = (ReadableSpan) span;
      if ("localhost".equals(readableSpan.getAttribute(ServerAttributes.SERVER_ADDRESS))) {
        // This makes context propagation only allowed if the active span server.address attribute
        // is "localhost", so it won't work with "::1" or "127.0.0.1".
        // Real-world use cases will probably need more complex logic and support
        // or prefixes, wildcards, ip addresses ranges, etc.
        allow = true;
      }

    }

    if(!allow) {
      return;
    }

    delegate.inject(context, carrier, setter);
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    return delegate.extract(context, carrier, getter);
  }
}
