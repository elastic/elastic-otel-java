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
    target = startTarget("tomcat:latest", container -> container
        .withExposedPorts(PORT)
        .waitingFor(Wait.forListeningPorts(PORT))
        .withCopyFileToContainer(MountableFile.forHostPath(testAppWar),
            "/usr/local/tomcat/webapps/app.war"));

    doRequest(getUrl("/app/"), okResponseBody("home sweet home"));

    checkTracesResources(attributes -> attributes
            .containsEntry("service.name", attributeValue("Test WAR application")),
        waitForTraces());
  }

  private static String getUrl(String path) {
    return getUrl(target, path, PORT);
  }
}
