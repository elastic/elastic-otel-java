package co.elastic.otel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

public class VirtualThreadSupportTest {

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  public void ensureSupported() {
    assertThat(JvmtiAccess.getVirtualThreadsUnsupportedReason()).isNull();
  }

  @Test
  @EnabledForJreRange(max = JRE.JAVA_20)
  public void ensureUnsupportedReasonPresent() {
    assertThat(JvmtiAccess.getVirtualThreadsUnsupportedReason())
        .contains("version");
  }
}
