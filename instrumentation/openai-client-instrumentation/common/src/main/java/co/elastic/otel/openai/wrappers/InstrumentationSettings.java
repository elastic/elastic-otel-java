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
package co.elastic.otel.openai.wrappers;

import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_ADDRESS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_PORT;

import io.opentelemetry.api.common.AttributesBuilder;

public class InstrumentationSettings {

  final boolean emitEvents;
  final boolean captureMessageContent;

  // we do not directly have access to the client baseUrl after construction, therefore we need to
  // remember it
  // visible for testing
  final String serverAddress;
  final Long serverPort;

  InstrumentationSettings(
      boolean emitEvents, boolean captureMessageContent, String serverAddress, Long serverPort) {
    this.emitEvents = emitEvents;
    this.captureMessageContent = captureMessageContent;
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }

  public void putServerInfoIntoAttributes(AttributesBuilder attributes) {
    if (serverAddress != null) {
      attributes.put(SERVER_ADDRESS, serverAddress);
    }
    if (serverPort != null) {
      attributes.put(SERVER_PORT, serverPort);
    }
  }
}
