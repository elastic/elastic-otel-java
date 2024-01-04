package co.elastic.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs({OS.LINUX, OS.MAC})
public class UniversalProfilingCorrelationTest {
  @AfterEach
  public void cleanUp() {
    JvmtiAccess.destroy();
  }

  @Test
  public void testProcessStorage() {
    ByteBuffer buffer = ByteBuffer.allocateDirect(100);
    buffer.order(ByteOrder.nativeOrder());
    buffer.asCharBuffer().put(0, "Hello World".toCharArray());

    UniversalProfilingCorrelation.setProcessStorage(buffer);

    ByteBuffer alias = JvmtiAccessImpl.createProcessProfilingCorrelationBufferAlias(100);
    alias.order(ByteOrder.nativeOrder());
    char[] data = new char[11];
    alias.asCharBuffer().get(data);

    assertThat(new String(data)).isEqualTo("Hello World");

    UniversalProfilingCorrelation.setProcessStorage(null);
    assertThat(JvmtiAccessImpl.createProcessProfilingCorrelationBufferAlias(100)).isNull();
  }

  @Test
  public void testPlatformThreadStorage() throws ExecutionException, InterruptedException {
    JvmtiAccess.assertInitialized();

    int numThreads = 10;
    CyclicBarrier threadsBarrier = new CyclicBarrier(10);

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    List<? extends Future<?>> futures = IntStream.range(1, numThreads + 1)
        .mapToObj(id -> executor.submit(() -> {
          //Ensure we don't allocate anything if specified to not allocate
          ByteBuffer buffer = UniversalProfilingCorrelation.getCurrentThreadStorage(false, 100);
          assertThat(buffer).isNull();
          assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100)).isNull();

          buffer = UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100);
          assertThat(buffer).isNotNull();
          assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100)).isNotNull();

          //subsequent calls should return the same instance
          assertThat(UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100))
              .isSameAs(buffer);

          buffer.putInt(0, id);

          try {
            threadsBarrier.await();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          ByteBuffer alias = JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100);
          alias.order(ByteOrder.nativeOrder());

          int readId = alias.getInt(0);
          assertThat(readId).isSameAs(id);

          UniversalProfilingCorrelation.removeCurrentThreadStorage();
          assertThat(UniversalProfilingCorrelation.getCurrentThreadStorage(false, 100)).isNull();
          assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100)).isNull();
        }))
        .collect(Collectors.toList());

    for (Future<?> future : futures) {
      //Rethrows any assertion errors occurring on the threads
      future.get();
    }

  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  public void testVirtualThreadsExcluded() throws Exception {
    ExecutorService exec = (ExecutorService) Executors.class.getMethod(
        "newVirtualThreadPerTaskExecutor").invoke(null);

    exec.submit(() -> {
      ByteBuffer buffer = UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100);
      assertThat(buffer).isNull();
      assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100)).isNull();
    }).get();

  }

}
