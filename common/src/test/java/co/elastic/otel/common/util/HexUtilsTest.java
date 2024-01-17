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

    HexUtils.writeHexAsBinary(
        "ignore0005abf1ignore",
        "ignore".length(),
        buffer,
        1,
        4
    );

    byte[] data = new byte[6];
    buffer.position(0);
    buffer.get(data);

    assertThat(data).containsExactly(42, 0x00, 0x05, 0xab, 0xf1, 42);
  }

}
