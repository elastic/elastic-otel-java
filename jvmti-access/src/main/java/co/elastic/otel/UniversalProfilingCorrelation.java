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

  static {
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

  static synchronized void reset() {
    threadStorage = new ThreadLocal<>();
    if (JvmtiAccess.isInitialized()) {
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
