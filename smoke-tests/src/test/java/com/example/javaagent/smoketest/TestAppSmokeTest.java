package com.example.javaagent.smoketest;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAppSmokeTest extends SmokeTest {

  private static final String IMAGE =
      "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:latest";

  private static GenericContainer<?> target;

  protected static void startApp() {
    startApp(Function.identity());
  }

  public static void startApp(Function<GenericContainer<?>, GenericContainer<?>> customizeContainer) {
    target = startContainer(IMAGE, container ->
        customizeContainer.apply(container
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health").forPort(8080))
        )
    );
  }

  protected static String getContainerId() {
    return target.getContainerId();
  }

  protected static void stopApp() {
    if (target != null) {
      target.stop();
      target = null;
    }
  }

  protected String getUrl(String path) {
    return getUrl(target, path, 8080);
  }

  protected void doRequest(String url, IOConsumer<Response> responseHandler) {
    Request request = new Request.Builder().url(url).get().build();

    try (Response response = client.newCall(request).execute()) {
      responseHandler.accept(response);
    } catch (IOException e){
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  protected interface IOConsumer<T> {
    void accept(T t) throws IOException;
  }

  protected static IOConsumer<Response> okResponseBody(String body) {
    return r -> {
      assertThat(r.code()).isEqualTo(200);
      assertThat(r.body()).isNotNull();
      assertThat(r.body().string()).isEqualTo(body);
    };
  }
}
