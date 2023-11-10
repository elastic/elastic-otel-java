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

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class AppServerProvidersTest extends SmokeTest {

  private static GenericContainer<?> target = null;

  private static Path testAppWar = null;

  // only testing with tomcat for now, thus always the same port
  private static final int PORT = 8080;

  @BeforeAll
  static void beforeAll() {
    String envPath = System.getProperty("io.opentelemetry.smoketest.agent.testAppWar.path");
    assertThat(envPath).describedAs("missing test war file").isNotNull();
    testAppWar = Path.of(envPath);
    assertThat(testAppWar).isRegularFile();
    assertThat(testAppWar.getFileName().toString()).endsWith(".war");
  }

  @AfterAll
  static void afterAll() {
    if (target != null) {
      target.close();
    }
  }

  // test with tomcat to ensure it's working, others are expected to be included
  @Test
  void tomcat() {
    target =
        startTarget(
            "tomcat:latest",
            container ->
                container
                    .withExposedPorts(PORT)
                    .waitingFor(Wait.forListeningPorts(PORT))
                    .withCopyFileToContainer(
                        MountableFile.forHostPath(testAppWar),
                        "/usr/local/tomcat/webapps/app.war"));

    doRequest(getUrl("/app/"), okResponseBody("home sweet home"));

    checkTracesResources(
        attributes ->
            attributes.containsEntry("service.name", attributeValue("Test WAR application")),
        waitForTraces());
  }

  private static String getUrl(String path) {
    return getUrl(target, path, PORT);
  }
}
