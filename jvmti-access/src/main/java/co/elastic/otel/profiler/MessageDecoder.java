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
package co.elastic.otel.profiler;

import java.nio.ByteBuffer;

public class MessageDecoder {
  private final TraceCorrelationMessage traceCorrelationMessage = new TraceCorrelationMessage();
  private final UnknownMessage unknownMessage = new UnknownMessage();

  public ProfilerMessage decode(ByteBuffer data) throws DecodeException {
    try {
      int messageType = data.getShort();
      data.getShort(); // message version, not used currently
      switch (messageType) {
        case TraceCorrelationMessage.TYPE_ID:
          return decode(traceCorrelationMessage, data);
        default:
          unknownMessage.messageType = messageType;
          return unknownMessage;
      }
    } catch (Exception e) {
      throw new DecodeException("Failed to decode message", e);
    }
  }

  private ProfilerMessage decode(TraceCorrelationMessage message, ByteBuffer data) {
    data.get(message.traceId);
    data.get(message.localRootSpanId);
    data.get(message.stackTraceId);
    message.sampleCount = data.getShort();
    return message;
  }
}
