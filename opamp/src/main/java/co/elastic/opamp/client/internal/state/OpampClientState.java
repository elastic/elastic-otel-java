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

import co.elastic.opamp.client.state.State;
import opamp.proto.Opamp;

public final class OpampClientState {
  public final RemoteConfigStatusState remoteConfigStatusState;
  public final SequenceNumberState sequenceNumberState;
  public final AgentDescriptionState agentDescriptionState;
  public final CapabilitiesState capabilitiesState;
  public final InstanceUidState instanceUidState;
  public final State<Opamp.EffectiveConfig> effectiveConfigState;

  public OpampClientState(
      RemoteConfigStatusState remoteConfigStatusState,
      SequenceNumberState sequenceNumberState,
      AgentDescriptionState agentDescriptionState,
      CapabilitiesState capabilitiesState,
      InstanceUidState instanceUidState,
      State<Opamp.EffectiveConfig> effectiveConfigState) {
    this.remoteConfigStatusState = remoteConfigStatusState;
    this.sequenceNumberState = sequenceNumberState;
    this.agentDescriptionState = agentDescriptionState;
    this.effectiveConfigState = effectiveConfigState;
    this.capabilitiesState = capabilitiesState;
    this.instanceUidState = instanceUidState;
  }
}
