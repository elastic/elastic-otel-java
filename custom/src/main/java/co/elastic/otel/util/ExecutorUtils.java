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
package co.elastic.otel.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ExecutorUtils {

  private static final Logger logger = Logger.getLogger(ExecutorUtils.class.getName());

  public static ThreadFactory threadFactory(String purpose, boolean daemon) {
    return r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(daemon);
      thread.setName(String.format("elastic-%s", purpose));
      return thread;
    };
  }

  public static void shutdownAndWaitTermination(ExecutorService executor) {
    shutdownAndWaitTermination(executor, 1, TimeUnit.SECONDS);
  }

  public static void shutdownAndWaitTermination(
      ExecutorService executor, long timeout, TimeUnit unit) {
    // Disable new tasks from being submitted
    executor.shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!executor.awaitTermination(timeout, unit)) {
        // Cancel currently executing tasks
        executor.shutdownNow();
        // Wait a while for tasks to respond to being cancelled
        if (!executor.awaitTermination(timeout, unit)) {
          logger.warning("Thread pool did not terminate in time " + executor);
        }
      }
    } catch (InterruptedException e) {
      // (Re-)Cancel if current thread also interrupted
      executor.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
