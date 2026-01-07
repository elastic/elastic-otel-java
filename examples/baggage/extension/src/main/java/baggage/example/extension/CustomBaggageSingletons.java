package baggage.example.extension;

import io.opentelemetry.context.ContextKey;

public class CustomBaggageSingletons {

  private CustomBaggageSingletons() {}

  private static final ContextKey<String> CONTEXT_KEY_HTTP_ROUTE = ContextKey.named("custom.http.route");

  public static ContextKey<String> httpRouteContextKey() {
    return CONTEXT_KEY_HTTP_ROUTE;
  }
}
