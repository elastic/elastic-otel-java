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

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.elastic.opamp.client.request.delay.PeriodicDelay;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PeriodicTaskExecutorTest {
  private static final int TIMEOUT_SECONDS = 5;

  @Test
  void verifyRunAtStart() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    PeriodicDelay delay = new TestPeriodicDelay(Duration.ofSeconds(0));

    PeriodicTaskExecutor.create(delay).start(latch::countDown);

    if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      fail("Timed out waiting");
    }
  }

  @Test
  void verifyRerunAfterFinishedRunning() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    PeriodicDelay delay = new TestPeriodicDelay(Duration.ofMillis(500));

    PeriodicTaskExecutor.create(delay).start(latch::countDown);

    if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      fail("Timed out waiting");
    }
  }

  @Test
  void verifyRunningImmediatelyWhenRequested() {
    PeriodicDelay delay = new TestPeriodicDelay(Duration.ofMinutes(1));
    ScheduledExecutorService executorService = mock();
    Runnable task = mock(Runnable.class);
    PeriodicTaskExecutor executor = new PeriodicTaskExecutor(executorService, delay);
    executor.start(task);

    executor.executeNow();

    verify(executorService).execute(task);
  }

  @Test
  void verifyKeepingOriginalScheduleAfterRunningImmediately() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    PeriodicDelay delay = new TestPeriodicDelay(Duration.ofSeconds(1));

    PeriodicTaskExecutor executor = PeriodicTaskExecutor.create(delay);
    executor.start(latch::countDown);

    executor.executeNow();

    if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      fail("Timed out waiting");
    }
  }

  @Test
  void verifyDelayValueUsed() {
    Duration duration = Duration.ofSeconds(1);
    PeriodicDelay delay = new TestPeriodicDelay(duration);
    ScheduledExecutorService executorService = mock();

    PeriodicTaskExecutor executor = new PeriodicTaskExecutor(executorService, delay);

    executor.start(() -> {});

    verify(executorService)
        .schedule(any(Runnable.class), eq(duration.toNanos()), eq(TimeUnit.NANOSECONDS));
  }

  @Test
  void verifyDelayChange() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    PeriodicDelay delay = new TestPeriodicDelay(Duration.ofMinutes(1));

    PeriodicTaskExecutor executor = PeriodicTaskExecutor.create(delay);
    executor.start(latch::countDown);

    executor.setPeriodicDelay(new TestPeriodicDelay(Duration.ofSeconds(1)));

    if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      fail("Timed out waiting");
    }
  }

  @Test
  void verifyPreviousTaskCancelledWhenDelayChanges() {
    ScheduledExecutorService executorService = mock();
    ScheduledFuture<?> firstTaskFuture = mock(ScheduledFuture.class);
    doReturn(firstTaskFuture).when(executorService).schedule(any(Runnable.class), anyLong(), any());

    PeriodicTaskExecutor executor =
        new PeriodicTaskExecutor(executorService, new TestPeriodicDelay(Duration.ofSeconds(1)));

    executor.start(() -> {});

    executor.setPeriodicDelay(new TestPeriodicDelay(Duration.ofSeconds(2)));

    verify(firstTaskFuture).cancel(false);
  }

  @Test
  void verifyNewPeriodicDelayGetsResetBeforeBeingUsed() {
    ScheduledExecutorService executorService = mock();
    ScheduledFuture<?> firstTaskFuture = mock(ScheduledFuture.class);
    doReturn(firstTaskFuture).when(executorService).schedule(any(Runnable.class), anyLong(), any());
    PeriodicDelay delayChange = mock();

    PeriodicTaskExecutor executor =
        new PeriodicTaskExecutor(executorService, new TestPeriodicDelay(Duration.ofSeconds(1)));

    executor.start(() -> {});

    executor.setPeriodicDelay(delayChange);

    InOrder inOrder = inOrder(delayChange);
    inOrder.verify(delayChange).reset();
    inOrder.verify(delayChange).getNextDelay();
  }

  @Test
  void verifyStop() {
    ScheduledExecutorService executorService = mock();

    PeriodicTaskExecutor executor =
        new PeriodicTaskExecutor(executorService, new TestPeriodicDelay(Duration.ofSeconds(1)));
    executor.stop();

    verify(executorService).shutdown();
  }

  private static class TestPeriodicDelay implements PeriodicDelay {
    private final Duration delay;

    private TestPeriodicDelay(Duration delay) {
      this.delay = delay;
    }

    @Override
    public Duration getNextDelay() {
      return delay;
    }

    @Override
    public void reset() {}
  }
}
