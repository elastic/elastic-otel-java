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
  public static synchronized ByteBuffer getCurrentThreadStorage(boolean allocateIfRequired,
      int expectedCapacity) {
    if (isVirtual(Thread.currentThread())) {
      return null; //virtual threads are not supported yet
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

  public static synchronized void removeCurrentThreadStorage() {
    if (isVirtual(Thread.currentThread())) {
      return; //virtual threads are not supported yet
    }
    ByteBuffer buffer = threadStorage.get();
    if (buffer != null) {
      JvmtiAccess.setProfilingCorrelationCurrentThreadStorage(null);
      threadStorage.remove();
    }
  }

  static synchronized void reset() {
    threadStorage = new ThreadLocal<>();
  }

  private static boolean isVirtual(Thread thread) {
    try {
      return (boolean) VIRTUAL_CHECKER.invokeExact(thread);
    } catch (Throwable e) {
      throw new IllegalStateException("isVirtual is not expected to throw exceptions", e);
    }
  }

  private static MethodHandle generateVirtualChecker() {
    Method isVirtual = null;
    try {
      isVirtual = Thread.class.getMethod("isVirtual");
      isVirtual.invoke(
          Thread.currentThread()); // invoke to ensure it does not throw exceptions for preview
      // versions
      return MethodHandles.lookup().unreflect(isVirtual);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // virtual threads are not supported, therefore no thread is virtual
      return MethodHandles.dropArguments(
          MethodHandles.constant(boolean.class, false), 0, Thread.class);
    }
  }
}
