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

import co.elastic.otel.profiler.DecodeException;
import co.elastic.otel.profiler.MessageDecoder;
import co.elastic.otel.profiler.ProfilerMessage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nullable;

public class UniversalProfilingCorrelation {

  private static final MethodHandle VIRTUAL_CHECKER = generateVirtualChecker();

  private static ThreadLocal<ByteBuffer> threadStorage;

  // We hold a reference to the configured processStorage to make sure it is not GCed
  private static ByteBuffer processStorage;

  private static final ByteBuffer messageBuffer;
  private static final MessageDecoder messageDecoder = new MessageDecoder();

  static {
    messageBuffer = ByteBuffer.allocateDirect(1024);
    messageBuffer.order(ByteOrder.nativeOrder());
    reset();
  }

  public static synchronized void setProcessStorage(@Nullable ByteBuffer buffer) {
    if (buffer != null) {
      if (!buffer.isDirect()) {
        throw new IllegalArgumentException("The bytebuffer must be direct!");
      }
      if (buffer.order() != ByteOrder.nativeOrder()) {
        throw new IllegalArgumentException("The bytebuffer must have native byteorder!");
      }
    }
    JvmtiAccess.setProfilingCorrelationProcessStorage(buffer);
    processStorage = buffer;
  }

  @Nullable
  public static ByteBuffer getCurrentThreadStorage(
      boolean allocateIfRequired, int expectedCapacity) {
    if (isVirtual(Thread.currentThread())) {
      return null; // virtual threads are not supported yet
    }
    ByteBuffer buffer = threadStorage.get();
    if (buffer == null) {
      if (!allocateIfRequired) {
        return null;
      }
      buffer = ByteBuffer.allocateDirect(expectedCapacity);
      buffer.order(ByteOrder.nativeOrder());
      JvmtiAccess.setProfilingCorrelationCurrentThreadStorage(buffer);
      threadStorage.set(buffer);
    }

    int actualCapacity = buffer.capacity();
    if (actualCapacity != expectedCapacity) {
      throw new IllegalArgumentException(
          "Buffer has been allocated with a different capacity: " + actualCapacity);
    }
    return buffer;
  }

  public static void removeCurrentThreadStorage() {
    if (isVirtual(Thread.currentThread())) {
      return; // virtual threads are not supported yet
    }
    ByteBuffer buffer = threadStorage.get();
    if (buffer != null) {
      try {
        JvmtiAccess.setProfilingCorrelationCurrentThreadStorage(null);
      } finally {
        threadStorage.remove();
      }
    }
  }

  /**
   * Starts and binds a unix socket at the given filepath to receive messages from the profiler.
   *
   * @param filepath the file to use for binding the socket
   */
  public static void startProfilerReturnChannel(String filepath) {
    JvmtiAccess.startProfilerReturnChannelSocket(filepath);
  }

  /** Stops and removes the socket created via {@link #stopProfilerReturnChannel()}. */
  public static void stopProfilerReturnChannel() {
    JvmtiAccess.stopProfilerReturnChannelSocket();
  }

  /**
   * Reads a message as bytearray from the profiler return channel socket into the provided
   * bytebuffer. If the provided buffer is smaller than the received message, the message will be
   * truncated.
   *
   * @param outputBuffer the buffer to read to. The position will be reset to 0 and the limit will
   *     be set to the size of the received message.
   * @return true, if a message was available and received. False if no message is available
   */
  static boolean readProfilerReturnChannelMessageBytes(ByteBuffer outputBuffer) {
    if (!outputBuffer.isDirect()) {
      throw new IllegalArgumentException("The provided bytebuffer is not direct");
    }
    if (outputBuffer.order() != ByteOrder.nativeOrder()) {
      throw new IllegalArgumentException("The provided bytebuffer does not have native byteorder");
    }
    int numBytesRead = JvmtiAccess.receiveProfilerReturnChannelMessage(outputBuffer);
    outputBuffer.position(0);
    outputBuffer.limit(numBytesRead);
    return numBytesRead > 0;
  }

  /**
   * Reads a message from the profiler return channel socket. If no message is available, null is
   * returned.
   *
   * <p>The returned message is a singleton and will be reused on subsequent invocations. Therefore,
   * the return value only remains valid until the next call of this method.
   *
   * @throws DecodeException if anything went wrong decoding the current message. This exception
   *     indicates that the call can be retried to fetch the next message.
   */
  @Nullable
  public static synchronized ProfilerMessage readProfilerReturnChannelMessage()
      throws DecodeException {
    if (readProfilerReturnChannelMessageBytes(messageBuffer)) {
      return messageDecoder.decode(messageBuffer);
    }
    return null;
  }

  static synchronized void reset() {
    threadStorage = new ThreadLocal<>();
    if (processStorage != null) {
      processStorage = null;
      JvmtiAccess.setProfilingCorrelationProcessStorage(null);
    }
  }

  private static boolean isVirtual(Thread thread) {
    try {
      return (boolean) VIRTUAL_CHECKER.invokeExact(thread);
    } catch (Throwable e) {
      throw new IllegalStateException("isVirtual is not expected to throw exceptions", e);
    }
  }

  /**
   * Generates a method handle which checks if a given thread is a virtual thread. We need to do
   * this via reflection (or MethodHandle as inline friendly alternative), because versions prior to
   * Java 21 don't hava Thread.isVirtual().
   */
  private static MethodHandle generateVirtualChecker() {
    Method isVirtual = null;
    try {
      isVirtual = Thread.class.getMethod("isVirtual");
      // invoke to ensure it does not throw exceptions for preview versions
      isVirtual.invoke(Thread.currentThread());
      return MethodHandles.lookup().unreflect(isVirtual);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // virtual threads are not supported, therefore no thread is virtual
      return MethodHandles.dropArguments(
          MethodHandles.constant(boolean.class, false), 0, Thread.class);
    }
  }
}
