package co.elastic.otel.common.testutils;

import java.util.ArrayList;
import java.util.List;

public class AssertionCollector {

  private final List<RuntimeException> collected = new ArrayList<>();

  public void collect(Runnable task) {
    try {
      task.run();
    } catch (RuntimeException e) {
      collected.add(e);
    }
  }

  public void rethrowFirst() {
    if (!collected.isEmpty()) {
      throw collected.get(0);
    }
  }
}
