package co.elastic.otel;

import org.junit.jupiter.api.Test;

public class JvmtiAccessTest {

  @Test
  void checkHello() {
    String s = JvmtiAccess.sayHello();
    System.out.println(s);
  }

}
