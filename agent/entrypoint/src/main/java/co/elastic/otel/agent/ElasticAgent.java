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

/** Elastic agent entry point, delegates to OpenTelemetry agent */
public class ElasticAgent {

  /**
   * Entry point for -javaagent JVM argument attach
   *
   * @param agentArgs agent arguments
   * @param inst instrumentation
   */
  public static void premain(String agentArgs, Instrumentation inst) {

    // must match value returned by ElasticLoggingCustomizer#getName
    System.setProperty("otel.javaagent.logging", "elastic");

    OpenTelemetryAgent.premain(agentArgs, inst);
  }

  /**
   * Entry point for runtime attach
   *
   * @param agentArgs agent arguments
   * @param inst instrumentation
   */
  public static void agentmain(String agentArgs, Instrumentation inst) {
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

  private ElasticAgent() {}
}
