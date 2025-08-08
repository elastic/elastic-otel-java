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
package co.elastic.otel.dynamicconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class ConfigLoggerTest {
  static final String TARGET_CLASS_NAME = "TempTest321";

  @Test
  public void checkGlobalOpenTelemetryString() throws IOException {
    String output = executeCommand(createTestTargetCommand(), 20);
    assertThat(output).contains("ConfigLogger - GlobalOpenTelemetry");
    assertThat(output).contains("tracerProvider=SdkTracerProvider");
  }

  static File createTestTarget() throws IOException {
    String targetClass =
        "public class "
            + TARGET_CLASS_NAME
            + " {\n"
            + "  public static void main(String[] args) {\n"
            + "    System.out.println(\"Test started\");\n"
            + "    try {Thread.sleep(6_000L);} catch (InterruptedException e) {}\n"
            + "    System.out.println(\"Test ended\");\n"
            + "  }\n"
            + "}\n";
    File targetClassFile =
        new File(System.getProperty("java.io.tmpdir"), TARGET_CLASS_NAME + ".java");
    targetClassFile.deleteOnExit();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetClassFile))) {
      writer.write(targetClass);
    }
    return targetClassFile;
  }

  static String getAgentJarFile() {
    // note File path with / is valid on Windows too
    File[] jar =
        new File("../agent/build/libs")
            .listFiles(
                (dir, name) ->
                    name.matches("elastic-otel-javaagent-\\d\\.\\d\\.\\d-SNAPSHOT\\.jar"));
    if (jar == null || jar.length != 1) {
      throw new IllegalStateException(
          "expecting exactly one agent jar file in ../agent/build/libs");
    }
    return jar[0].getAbsolutePath();
  }

  static List<String> createTestTargetCommand() throws IOException {
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("-Xmx32m");
    command.add("-javaagent:" + getAgentJarFile());
    command.add("-Delastic.otel.java.configuration.log.delay.seconds=5");
    command.add(createTestTarget().getAbsolutePath());
    return command;
  }

  private static void pauseSeconds(int seconds) {
    try {
      Thread.sleep(seconds * 1_000L);
    } catch (InterruptedException e) {
    }
  }

  public static String executeCommand(List<String> command, int timeoutSeconds) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process childProcess = pb.start();

    StringBuilder commandOutput = new StringBuilder();

    boolean isAlive = true;
    byte[] buffer = new byte[64 * 1000];
    InputStream in = childProcess.getInputStream();
    // stop trying if the time elapsed exceeds the timeout
    while (isAlive && (timeoutSeconds > 0)) {
      while (in.available() > 0) {
        int lengthRead = in.read(buffer, 0, buffer.length);
        commandOutput.append(new String(buffer, 0, lengthRead));
      }
      pauseSeconds(1);
      timeoutSeconds--;
      isAlive = childProcess.isAlive();
    }
    // it can die but still have output available buffered
    while (in.available() > 0) {
      int lengthRead = in.read(buffer, 0, buffer.length);
      commandOutput.append(new String(buffer, 0, lengthRead));
    }

    // Cleanup as well as I can
    boolean exited = false;
    try {
      exited = childProcess.waitFor(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
    }
    if (!exited) {
      childProcess.destroy();
      pauseSeconds(1);
      if (childProcess.isAlive()) {
        childProcess.destroyForcibly();
      }
    }

    return commandOutput.toString();
  }
}
