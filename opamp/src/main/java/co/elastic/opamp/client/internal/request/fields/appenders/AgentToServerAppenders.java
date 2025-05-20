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
package co.elastic.opamp.client.internal.request.fields.appenders;

import co.elastic.opamp.client.internal.request.fields.FieldType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AgentToServerAppenders {
  public final AgentDescriptionAppender agentDescriptionAppender;
  public final EffectiveConfigAppender effectiveConfigAppender;
  public final RemoteConfigStatusAppender remoteConfigStatusAppender;
  public final SequenceNumberAppender sequenceNumberAppender;
  public final CapabilitiesAppender capabilitiesAppender;
  public final InstanceUidAppender instanceUidAppender;
  public final FlagsAppender flagsAppender;
  public final AgentDisconnectAppender agentDisconnectAppender;
  private final Map<FieldType, AgentToServerAppender> allAppenders;

  public AgentToServerAppenders(
      AgentDescriptionAppender agentDescriptionAppender,
      EffectiveConfigAppender effectiveConfigAppender,
      RemoteConfigStatusAppender remoteConfigStatusAppender,
      SequenceNumberAppender sequenceNumberAppender,
      CapabilitiesAppender capabilitiesAppender,
      InstanceUidAppender instanceUidAppender,
      FlagsAppender flagsAppender,
      AgentDisconnectAppender agentDisconnectAppender) {
    this.agentDescriptionAppender = agentDescriptionAppender;
    this.effectiveConfigAppender = effectiveConfigAppender;
    this.remoteConfigStatusAppender = remoteConfigStatusAppender;
    this.sequenceNumberAppender = sequenceNumberAppender;
    this.capabilitiesAppender = capabilitiesAppender;
    this.instanceUidAppender = instanceUidAppender;
    this.flagsAppender = flagsAppender;
    this.agentDisconnectAppender = agentDisconnectAppender;

    Map<FieldType, AgentToServerAppender> appenders = new HashMap<>();
    appenders.put(FieldType.AGENT_DESCRIPTION, agentDescriptionAppender);
    appenders.put(FieldType.EFFECTIVE_CONFIG, effectiveConfigAppender);
    appenders.put(FieldType.REMOTE_CONFIG_STATUS, remoteConfigStatusAppender);
    appenders.put(FieldType.SEQUENCE_NUM, sequenceNumberAppender);
    appenders.put(FieldType.CAPABILITIES, capabilitiesAppender);
    appenders.put(FieldType.INSTANCE_UID, instanceUidAppender);
    appenders.put(FieldType.FLAGS, flagsAppender);
    appenders.put(FieldType.AGENT_DISCONNECT, agentDisconnectAppender);
    allAppenders = Collections.unmodifiableMap(appenders);
  }

  public AgentToServerAppender getForField(FieldType fieldType) {
    if (!allAppenders.containsKey(fieldType)) {
      throw new IllegalArgumentException("Field type " + fieldType + " is not supported");
    }
    return allAppenders.get(fieldType);
  }
}
