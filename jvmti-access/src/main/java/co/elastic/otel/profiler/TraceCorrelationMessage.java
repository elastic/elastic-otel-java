package co.elastic.otel.profiler;

import java.util.Arrays;

public class TraceCorrelationMessage implements ProfilerMessage {

  static final int TYPE_ID = 1;
  final byte[] traceId = new byte[16];
  final byte[] localRootSpanId = new byte[8];
  final byte[] stackTraceId = new byte[8];
  int sampleCount;

  /**
   * @return the 16 byte trace id for which a CPU profiling sample was taken
   */
  public byte[] getTraceId() {
    return traceId;
  }

  /**
   * @return the 8 byte span id of the local root span for which a CPU profiling sample was taken
   */
  public byte[] getLocalRootSpanId() {
    return localRootSpanId;
  }

  /**
   * @return the 8 byte id of the stack trace observed by the profiler
   */
  public byte[] getStackTraceId() {
    return stackTraceId;
  }

  /**
   * @return the total number of samples with the given trace-id, local-root-span-id and stacktrace-id observed since the last report
   */
  public int getSampleCount() {
    return sampleCount;
  }

  @Override
  public String toString() {
    return "TraceCorrelationMessage{" +
        "traceId=" + Arrays.toString(traceId) +
        ", localRootSpanId=" + Arrays.toString(localRootSpanId) +
        ", stackTraceId=" + Arrays.toString(stackTraceId) +
        ", sampleCount=" + sampleCount +
        '}';
  }
}
