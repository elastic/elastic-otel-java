package co.elastic.otel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs({OS.LINUX, OS.MAC})
public class JvmtiAccessTest {

  @AfterEach
  public void cleanUp() {
    JvmtiAccess.destroy();
  }

  @Test
  void checkHello() {
    String s = JvmtiAccess.sayHello();
    assertThat(s).isEqualTo("Hello from native");
  }

}
