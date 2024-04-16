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
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DistroResourceAttributesTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  public void checkDistroName() {
    Span testSpan =
        GlobalOpenTelemetry.get().getTracer("dummy-tracer").spanBuilder("testSpan").startSpan();
    testSpan.end();
    assertThat(testing.spans()).hasSize(1);
    Resource resource = testing.spans().get(0).getResource();

    boolean isRunningDistro = System.getProperty("otel.javaagent.extensions") == null;

    if (isRunningDistro) {
      assertThat(resource.getAttributes()).containsEntry("telemetry.distro.name", "elastic");
      assertThat(resource.getAttributes()).containsKey("telemetry.distro.version");
    } else {
      // we are running with the vanilla agent as extension: we should not be setting the distro
      // name
      assertThat(resource.getAttributes()).doesNotContainKey("telemetry.distro.name");
      assertThat(resource.getAttributes()).doesNotContainKey("telemetry.distro.version");
    }
  }
}
