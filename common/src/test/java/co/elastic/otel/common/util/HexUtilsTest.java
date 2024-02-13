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
package co.elastic.otel.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

public class HexUtilsTest {

  @Test
  public void writeHexToByteBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(6);

    buffer.put(0, (byte) 42);
    buffer.put(5, (byte) 42);

    HexUtils.writeHexAsBinary("ignore0005abf1ignore", "ignore".length(), buffer, 1, 4);

    byte[] data = new byte[6];
    buffer.position(0);
    buffer.get(data);

    assertThat(data).containsExactly(42, 0x00, 0x05, 0xab, 0xf1, 42);
  }

  @Test
  public void bytesToHexString() {
    StringBuilder result = new StringBuilder();

    HexUtils.appendAsHex(new byte[] {0x01, (byte) 0xAB, (byte) 0xFF}, result);

    assertThat(result.toString()).isEqualTo("01abff");
  }

}
