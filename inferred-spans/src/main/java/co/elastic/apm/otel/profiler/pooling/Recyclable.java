package co.elastic.apm.otel.profiler.pooling;

public interface Recyclable {

  /**
   * resets pooled object state so it can be reused
   */
  void resetState();

}
