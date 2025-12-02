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
package co.elastic.otel.agent;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Elastic agent entry point, delegates to OpenTelemetry agent */
public class ElasticAgent {

  private static final String OTEL_JAVAAGENT_LOGGING = "otel.javaagent.logging";
  private static final String OTEL_JAVAAGENT_LOGGING_ENV = "OTEL_JAVAAGENT_LOGGING";
  private static final String OTEL_JAVAAGENT_LOGGING_DEFAULT = "simple";

  /**
   * Entry point for -javaagent JVM argument attach
   *
   * @param agentArgs agent arguments
   * @param inst instrumentation
   */
  public static void premain(String agentArgs, Instrumentation inst) {
    ensureAccessToMethodHandles();
    initLogging();
    OpenTelemetryAgent.premain(agentArgs, inst);
  }

  /**
   * Entry point for runtime attach
   *
   * @param agentArgs agent arguments
   * @param inst instrumentation
   */
  public static void agentmain(String agentArgs, Instrumentation inst) {
    ensureAccessToMethodHandles();
    initLogging();
    OpenTelemetryAgent.agentmain(agentArgs, inst);
  }

  /**
   * Entry point to execute as program
   *
   * @param args arguments
   */
  public static void main(String[] args) {
    OpenTelemetryAgent.main(args);
  }

  private static void ensureAccessToMethodHandles() {
    // This is required to prevent a ClassCircularityError on Java 17+ when loading the logging
    // subsystem.
    // The issue is that the logging subsystem uses MethodHandles, and if MethodHandles are not
    // loaded yet, the first use might trigger a class load that eventually triggers the logging
    // subsystem again (e.g. through instrumentation).
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      MethodHandle mh =
          lookup.findVirtual(String.class, "length", MethodType.methodType(int.class));
      mh.invokeExact("string");
    } catch (Throwable e) {
      // ignore
    }
  }

  private static void initLogging() {

    // Do not override explicitly provided configuration unless it's using the default as the
    // 'simple' provider is not included in this distribution and that triggers an SLF4j error.
    if (isLoggingNotDefault(System.getProperty(OTEL_JAVAAGENT_LOGGING))
        || isLoggingNotDefault(System.getenv(OTEL_JAVAAGENT_LOGGING_ENV))) {
      return;
    }

    // must match value returned by ElasticLoggingCustomizer#getName
    System.setProperty(OTEL_JAVAAGENT_LOGGING, "elastic");
  }

  private static boolean isLoggingNotDefault(String value) {
    return value != null && !OTEL_JAVAAGENT_LOGGING_DEFAULT.equals(value);
  }

  private ElasticAgent() {}
}
