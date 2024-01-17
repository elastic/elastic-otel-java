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
package co.elastic.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      int id = i;
      futures.add(
          executor.submit(
              () -> {
                // Ensure we don't allocate anything if specified to not allocate
                ByteBuffer buffer =
                    UniversalProfilingCorrelation.getCurrentThreadStorage(false, 100);
                assertThat(buffer).isNull();
                assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100))
                    .isNull();

                buffer = UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100);
                assertThat(buffer).isNotNull();
                assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100))
                    .isNotNull();

                // subsequent calls should return the same instance
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
                assertThat(UniversalProfilingCorrelation.getCurrentThreadStorage(false, 100))
                    .isNull();
                assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100))
                    .isNull();
              }));
    }
    for (Future<?> future : futures) {
      // Rethrows any assertion errors occurring on the threads
      future.get();
    }
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  public void testVirtualThreadsExcluded() throws Exception {
    ExecutorService exec =
        (ExecutorService) Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);

    exec.submit(
            () -> {
              ByteBuffer buffer = UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100);
              assertThat(buffer).isNull();
              assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100)).isNull();
            })
        .get();
  }
}
