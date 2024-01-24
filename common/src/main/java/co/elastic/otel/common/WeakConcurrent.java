/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

  private static final WeakConcurrentSet<WeakConcurrentMap<?, ?>> registeredMaps =
      new WeakConcurrentSet<>(WeakConcurrentSet.Cleaner.MANUAL);

  private static volatile ScheduledFuture<?> cleaningTask = null;

  /**
   * Creates a new {@link WeakConcurrentMap} which is periodically cleaned up by a shared background
   * thread.
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
          ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(threadFactory);
          cleaningTask =
              exec.scheduleAtFixedRate(
                  WeakConcurrent::expungeStaleEntries,
                  CLEANUP_FREQUENCY.toMillis(),
                  CLEANUP_FREQUENCY.toMillis(),
                  TimeUnit.MILLISECONDS);
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
