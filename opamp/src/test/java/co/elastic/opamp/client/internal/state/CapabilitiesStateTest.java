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
package co.elastic.opamp.client.internal.state;

import static org.assertj.core.api.Assertions.assertThat;

import opamp.proto.Opamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapabilitiesStateTest {
  private CapabilitiesState capabilitiesState;

  @BeforeEach
  void setUp() {
    capabilitiesState = CapabilitiesState.create();
  }

  @Test
  void verifyDefaultValue() {
    assertThat(hasFlag(Opamp.AgentCapabilities.AgentCapabilities_ReportsStatus_VALUE)).isTrue();
  }

  @Test
  void verifyAddingAndRemovingCapabilities() {
    int capability = Opamp.AgentCapabilities.AgentCapabilities_AcceptsPackages_VALUE;
    assertThat(hasFlag(capability)).isFalse();

    capabilitiesState.add(capability);
    assertThat(hasFlag(capability)).isTrue();
    assertThat(hasFlag(Opamp.AgentCapabilities.AgentCapabilities_ReportsStatus_VALUE)).isTrue();

    capabilitiesState.remove(capability);
    assertThat(hasFlag(capability)).isFalse();
    assertThat(hasFlag(Opamp.AgentCapabilities.AgentCapabilities_ReportsStatus_VALUE)).isTrue();
  }

  private boolean hasFlag(long flag) {
    return (capabilitiesState.get() & flag) == flag;
  }
}
