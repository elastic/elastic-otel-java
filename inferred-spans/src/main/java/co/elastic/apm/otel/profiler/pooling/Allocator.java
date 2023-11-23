package co.elastic.apm.otel.profiler.pooling;

/**
 * Defines pooled object factory
 *
 * @param <T> pooled object type
 */
public interface Allocator<T> {

  /**
   * @return new instance of pooled object type
   */
  T createInstance();
}
