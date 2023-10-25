/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.example.javaagent.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import okhttp3.Request;
import okhttp3.Response;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class TestAppSmokeTest extends SmokeTest {

  private static final String IMAGE =
      "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:latest";

  private static GenericContainer<?> target;

  protected static void startApp() {
    startApp((container) -> {
    });
  }

  public static void startApp(Consumer<GenericContainer<?>> customizeContainer) {
    target = startContainer(
            IMAGE,
            customizeContainer.andThen(container -> container
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/health").forPort(8080))));
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
    } catch (IOException e) {
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
