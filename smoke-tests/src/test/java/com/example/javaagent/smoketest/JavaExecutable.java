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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaExecutable {

  private static final Logger log = LoggerFactory.getLogger(JavaExecutable.class);

  private JavaExecutable() {}

  /**
   * @return absolute path to current Java executable
   */
  public static String getBinaryPath() {
    boolean isWindows = System.getProperty("os.name").startsWith("Windows");
    String executable = isWindows ? "java.exe" : "java";
    Path path = Paths.get(System.getProperty("java.home"), "bin", executable);
    if (!Files.isExecutable(path)) {
      throw new IllegalStateException("unable to find java path " + path);
    }
    return path.toAbsolutePath().toString();
  }

  /**
   * @return {@literal true} when the current JVM is being debugged (with `-agentlib:...`
   *     parameter).
   */
  public static boolean isDebugging() {
    // test if the test code is currently being debugged
    List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (String jvmArg : jvmArgs) {
      if (jvmArg.contains("-agentlib:jdwp=")) {
        return true;
      }
    }
    return false;
  }

  private static boolean probeListeningDebugger(int port) {
    // the most straightforward way to probe for an active debugger listening on port is to start
    // another JVM
    // with the debug options and check the process exit status. Trying to probe for open network
    // port messes with
    // the debugger and makes IDEA stop it. The only downside of this is that the debugger will
    // first attach to this
    // probe JVM, then the one running in a docker container we are aiming to debug.
    try {
      Process process =
          new ProcessBuilder()
              .command(
                  JavaExecutable.getBinaryPath().toString(),
                  jvmDebugArgument("localhost", port),
                  "-version")
              .start();
      process.waitFor(5, TimeUnit.SECONDS);
      return process.exitValue() == 0;
    } catch (InterruptedException | IOException e) {
      return false;
    }
  }

  public static String jvmDebugArgument(String host, int port) {
    return String.format(
        "-agentlib:jdwp=transport=dt_socket,server=n,address=%s:%d,suspend=y", host, port);
  }

  public static String jvmAgentArgument(String path) {
    return "-javaagent:" + path;
  }

  public static boolean isListeningDebuggerStarted(int port, String description) {

    if (probeListeningDebugger(port)) {
      log.info(
          "listening debugger detected on port {}, remote debugging in the {} container is enabled",
          port,
          description);
      return true;
    } else {
      log.info(
          "listening debugger not detected on port {}, remote debugging in the {} container is not enabled",
          port,
          description);
      return false;
    }
  }
}
