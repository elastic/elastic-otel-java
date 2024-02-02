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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import co.elastic.otel.profiler.ProfilerMessage;
import co.elastic.otel.profiler.TraceCorrelationMessage;
import co.elastic.otel.profiler.UnknownMessage;
import java.lang.ref.WeakReference;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@EnabledOnOs({OS.LINUX, OS.MAC})
public class UniversalProfilingCorrelationTest {
  @AfterEach
  public void cleanUp() {
    JvmtiAccess.destroy();
  }

  @Nested
  class CorrelationMemory {

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
    public void ensureProcessStorageBufferNotGCed() {
      ByteBuffer gcMeEarly = ByteBuffer.allocateDirect(1000);
      ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
      buffer.order(ByteOrder.nativeOrder());

      UniversalProfilingCorrelation.setProcessStorage(buffer);

      WeakReference weakGcMe = new WeakReference(gcMeEarly);
      WeakReference weakBuf = new WeakReference(buffer);

      gcMeEarly = null;
      buffer = null;

      await()
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(1))
          .untilAsserted(
              () -> {
                System.gc();
                assertThat(weakGcMe.get()).isNull();
              });

      assertThat(weakBuf.get()).isNotNull();
      UniversalProfilingCorrelation.setProcessStorage(null);

      await()
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(1))
          .untilAsserted(
              () -> {
                System.gc();
                assertThat(weakBuf.get()).isNull();
              });
    }

    @Test
    public void testPlatformThreadStorage() throws ExecutionException, InterruptedException {
      JvmtiAccess.ensureInitialized();

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

                  ByteBuffer alias =
                      JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100);
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
          (ExecutorService)
              Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);

      exec.submit(
              () -> {
                ByteBuffer buffer =
                    UniversalProfilingCorrelation.getCurrentThreadStorage(true, 100);
                assertThat(buffer).isNull();
                assertThat(JvmtiAccessImpl.createThreadProfilingCorrelationBufferAlias(100))
                    .isNull();
              })
          .get();
    }
  }

  @Nested
  class ProfilerSocket {

    @Test
    public void emptyFileName() {
      assertThatThrownBy(() -> UniversalProfilingCorrelation.startProfilerReturnChannel(""))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Could not bind socket");
    }

    @Test
    public void tooLongFileName() {
      StringBuilder name = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        name.append("abc");
      }
      assertThatThrownBy(
              () -> UniversalProfilingCorrelation.startProfilerReturnChannel(name.toString()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("filename");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void restartSocket(boolean restartOnSameFile, @TempDir Path tempDir) {
      String socketFile = tempDir.resolve("socketfile").toAbsolutePath().toString();

      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(new byte[] {1, 2});

      UniversalProfilingCorrelation.stopProfilerReturnChannel();
      if (!restartOnSameFile) {
        socketFile = tempDir.resolve("socketfile2").toAbsolutePath().toString();
      }

      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      ByteBuffer receiveBuffer = createDirectBuffer(100);

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessageBytes(receiveBuffer))
          .isFalse();

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(new byte[] {3, 4});
      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessageBytes(receiveBuffer))
          .isTrue();

      assertThat(receiveBuffer.limit()).isEqualTo(2);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 3);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 4);
    }

    @Test
    public void checkTruncatedMessageHandling(@TempDir Path tempDir) {
      String socketFile = tempDir.resolve("socketfile").toAbsolutePath().toString();

      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(new byte[] {1, 2, 3, 4});
      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(new byte[] {5, 6});

      // the first truncated message should not impact the second fitting one
      ByteBuffer receiveBuffer = createDirectBuffer(3);
      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessageBytes(receiveBuffer))
          .isTrue();
      assertThat(receiveBuffer.limit()).isEqualTo(3);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 1);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 2);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 3);

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessageBytes(receiveBuffer))
          .isTrue();
      assertThat(receiveBuffer.limit()).isEqualTo(2);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 5);
      assertThat(receiveBuffer.get()).isEqualTo((byte) 6);

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessageBytes(receiveBuffer))
          .isFalse();
    }

    @Test
    public void receiveUnknownMessage(@TempDir Path tempDir) {
      String socketFile = tempDir.resolve("socketfile").toAbsolutePath().toString();
      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      ByteBuffer dummyMessage = ByteBuffer.allocate(4);
      dummyMessage.order(ByteOrder.nativeOrder());
      dummyMessage.putShort((short) 42); // message-type
      dummyMessage.putShort((short) 1); // message-version

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(dummyMessage.array());

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessage())
          .isInstanceOf(UnknownMessage.class)
          .satisfies(msg -> assertThat(((UnknownMessage) msg).getMessageType()).isEqualTo(42));

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessage()).isNull();
    }

    @Test
    public void receiveTraceCorrelationMessage(@TempDir Path tempDir) {
      String socketFile = tempDir.resolve("socketfile").toAbsolutePath().toString();
      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      byte[] traceId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      byte[] rootSpanId = new byte[] {17, 18, 19, 20, 21, 22, 23, 24};
      byte[] sampleId = new byte[] {25, 26, 27, 28, 29, 30, 31, 32};

      ByteBuffer dummyMessage = ByteBuffer.allocate(38);
      dummyMessage.order(ByteOrder.nativeOrder());
      dummyMessage.putShort((short) 1); // message-type
      dummyMessage.putShort((short) 1); // message-version
      dummyMessage.put(traceId);
      dummyMessage.put(rootSpanId);
      dummyMessage.put(sampleId);
      dummyMessage.putShort((short) 42);

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(dummyMessage.array());

      ProfilerMessage msg = UniversalProfilingCorrelation.readProfilerReturnChannelMessage();
      assertThat(msg).isInstanceOf(TraceCorrelationMessage.class);

      TraceCorrelationMessage correl = (TraceCorrelationMessage) msg;
      assertThat(correl.getTraceId()).containsExactly(traceId);
      assertThat(correl.getLocalRootSpanId()).containsExactly(rootSpanId);
      assertThat(correl.getStackTraceId()).containsExactly(sampleId);
      assertThat(correl.getSampleCount()).isEqualTo(42);

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessage()).isNull();
    }

    @Test
    public void decodeTruncatedMessage(@TempDir Path tempDir) {
      String socketFile = tempDir.resolve("socketfile").toAbsolutePath().toString();
      UniversalProfilingCorrelation.startProfilerReturnChannel(socketFile);

      ByteBuffer dummyMessage = ByteBuffer.allocate(8);
      dummyMessage.order(ByteOrder.nativeOrder());
      dummyMessage.putShort((short) 1); // message-type
      dummyMessage.putShort((short) 1); // message-version
      dummyMessage.put(new byte[] {1, 2, 3, 4});

      JvmtiAccessImpl.sendToProfilerReturnChannelSocket0(dummyMessage.array());

      assertThatThrownBy(UniversalProfilingCorrelation::readProfilerReturnChannelMessage)
          .isInstanceOf(BufferUnderflowException.class);

      assertThat(UniversalProfilingCorrelation.readProfilerReturnChannelMessage()).isNull();
    }

    private ByteBuffer createDirectBuffer(int size) {
      ByteBuffer result = ByteBuffer.allocateDirect(size);
      result.order(ByteOrder.nativeOrder());
      return result;
    }
  }
}
