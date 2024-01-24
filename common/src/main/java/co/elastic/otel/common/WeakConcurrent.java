package co.elastic.otel.common;

import co.elastic.otel.common.util.ExecutorUtils;
import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentSet;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeakConcurrent {

  private static final Logger logger = Logger.getLogger(WeakConcurrent.class.getName());

  private static final Duration CLEANUP_FREQUENCY = Duration.ofMillis(100);

  private static final WeakConcurrentSet<WeakConcurrentMap<?, ?>> registeredMaps
      = new WeakConcurrentSet<>(WeakConcurrentSet.Cleaner.MANUAL);


  private static volatile ScheduledFuture<?> cleaningTask = null;

  /**
   * Creates a new {@link WeakConcurrentMap} which is periodically cleaned up by a shared
   * background thread.
   */
  public static <K, V> WeakConcurrentMap<K, V> createMap() {
    WeakConcurrentMap<K, V> result = new WeakConcurrentMap<>(false);
    registeredMaps.add(result);
    ensureCleaningTaskStarted();
    return result;
  }

  private static void ensureCleaningTaskStarted() {
    if (cleaningTask == null) {
      synchronized (WeakConcurrent.class) {
        if (cleaningTask == null) {
          ThreadFactory threadFactory = ExecutorUtils.threadFactory("weakmap-cleaner", true);
          ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(
              threadFactory);
          cleaningTask = exec.scheduleAtFixedRate(
              WeakConcurrent::expungeStaleEntries,
              CLEANUP_FREQUENCY.toMillis(),
              CLEANUP_FREQUENCY.toMillis(),
              TimeUnit.MILLISECONDS
          );
        }
      }
    }
  }

  private static void expungeStaleEntries() {
    try {
      registeredMaps.expungeStaleEntries();
      for (WeakConcurrentMap<?, ?> map : registeredMaps) {
        map.expungeStaleEntries();
      }
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Failed to cleanup weak maps", t);
    }
  }
}
