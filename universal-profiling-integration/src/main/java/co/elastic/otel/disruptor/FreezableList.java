package co.elastic.otel.disruptor;

import java.util.ArrayList;
import java.util.List;

public class FreezableList<T> {

  private List<T> list = new ArrayList<>();
  private boolean isFrozen = false;

  public synchronized void addIfNotFrozen(T value) {
    if (isFrozen) {
      return;
    }
    list.add(value);
  }

  public synchronized List<T> freezeAndGet() {
    isFrozen = true;
    return list;
  }
}
