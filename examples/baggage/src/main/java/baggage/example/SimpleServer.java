package baggage.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

public class SimpleServer {

  private final HttpServer server;
  private final String url;

  private SimpleServer(HttpServer server, String url) {
    this.server = server;
    this.url = url;
  }

  private static SimpleServer create(int port, String path, HttpHandler handler)
      throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(path, handler);
    server.setExecutor(Executors.newCachedThreadPool());
    return new SimpleServer(server, String.format("http://localhost:%d%s", port, path));
  }

  public static SimpleServer createBackend() throws IOException {
    return create(9000, "/backend/", exchange -> {

      // Authentication is assumed to be implemented by the gateway
      // the backend only needs to have a technical ID for customer.
      String customerId = exchange.getRequestHeaders().getFirst("customer_id");

      String response = String.format("hello from backend, customer_id=%s\n", customerId);
      stringResponse(exchange, 200, response);
    });
  }

  public static SimpleServer createGateway() throws IOException {

    HttpClient client = HttpClient.newHttpClient();
    return create(8000, "/gateway/", exchange -> {

      // emulate authentication from header, don't do this in production
      String secretPrefix = "secret=";
      String customerAuth = exchange.getRequestHeaders().getFirst("Authorization");
      String customerId = null;
      if (customerAuth.startsWith(secretPrefix)) {
        customerId = customerAuth.substring(secretPrefix.length());
      }
      if (customerId == null) {
        stringResponse(exchange, 401, "auth error");
        return;
      }

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://localhost:9000/backend/"))
          .header("customer_id", customerId)
          .build();

      // add new entries to current baggage
      Baggage baggage = Baggage.current().toBuilder()
          .put("example.customer.id", customerId)
          .put("example.customer.name", String.format("my-awesome-customer-%s", customerId))
          .build();

      // create a new context with the updated baggage and make it current for the backend HTTP call
      Context contextWithBaggage = baggage.storeInContext(Context.current());

      try (Scope scope = contextWithBaggage.makeCurrent()) {

        // call backend
        HttpResponse<String> response;
        try {
          response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
          stringResponse(exchange, 500, "internal error");
          return;
        }

        stringResponse(exchange, 200, response.body());
      }
    });
  }

  private static void stringResponse(HttpExchange exchange, int statusCode, String response)
      throws IOException {
    exchange.sendResponseHeaders(statusCode, response.length());
    OutputStream os = exchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  public String getUrl() {return this.url;}

  public void start() {
    server.start();
  }

  public void stop() {
    server.stop(1);
  }
}
