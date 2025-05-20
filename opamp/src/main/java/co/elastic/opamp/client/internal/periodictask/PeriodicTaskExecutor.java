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
package co.elastic.opamp.client.internal.periodictask;

import co.elastic.opamp.client.request.delay.PeriodicDelay;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class PeriodicTaskExecutor {
  private final ScheduledExecutorService executorService;
  private final Lock delaySetLock = new ReentrantLock();
  private PeriodicDelay periodicDelay;
  private ScheduledFuture<?> scheduledFuture;
  private Runnable periodicTask;

  public static PeriodicTaskExecutor create(PeriodicDelay initialPeriodicDelay) {
    return new PeriodicTaskExecutor(
        Executors.newSingleThreadScheduledExecutor(), initialPeriodicDelay);
  }

  PeriodicTaskExecutor(
      ScheduledExecutorService executorService, PeriodicDelay initialPeriodicDelay) {
    this.executorService = executorService;
    this.periodicDelay = initialPeriodicDelay;
  }

  public void start(Runnable periodicTask) {
    this.periodicTask = periodicTask;
    scheduleNext();
  }

  public void executeNow() {
    executorService.execute(periodicTask);
  }

  public void setPeriodicDelay(PeriodicDelay periodicDelay) {
    delaySetLock.lock();
    try {
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      this.periodicDelay = periodicDelay;
      periodicDelay.reset();
      scheduleNext();
    } finally {
      delaySetLock.unlock();
    }
  }

  public void stop() {
    executorService.shutdown();
  }

  private void scheduleNext() {
    delaySetLock.lock();
    try {
      scheduledFuture =
          executorService.schedule(
              new PeriodicRunner(), periodicDelay.getNextDelay().toNanos(), TimeUnit.NANOSECONDS);
    } finally {
      delaySetLock.unlock();
    }
  }

  private class PeriodicRunner implements Runnable {
    @Override
    public void run() {
      periodicTask.run();
      scheduleNext();
    }
  }
}
