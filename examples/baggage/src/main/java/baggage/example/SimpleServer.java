package baggage.example;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
      exchange.sendResponseHeaders(200, response.length());
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    });
  }

  public static SimpleServer createGateway() throws IOException {
    return create(8000, "/gateway/", exchange -> {

    });
  }

  public String getUrl() {return this.url;}

  public void start() {
    server.start();
  }

  public void stop() {
    server.stop(1);
  }
}
