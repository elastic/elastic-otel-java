package co.elastic.apm.otel.profiler;

import co.elastic.apm.otel.profiler.util.ByteUtils;
import co.elastic.apm.otel.profiler.util.HexUtils;
import co.elastic.apm.otel.profiler.pooling.Recyclable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import javax.annotation.Nullable;

/**
 * A mutable (and therefore recyclable) class storing the relevant bits of {@link SpanContext}
 * for generating inferred spans. Also stores a clock-anchor for the corresponding span obtained
 * via {@link NanoClock#getAnchor(Span)}.
 */
public class TraceContext implements Recyclable {

  public static final int SERIALIZED_LENGTH = 16 + 8 + 1 + 8;
  private long traceIdLow;
  private long traceIdHigh;
  private long id;
  private byte flags;

  private long clockAnchor;

  public TraceContext() {
  }

  // For testing only
  static TraceContext fromSpanContextWithZeroClockAnchor(SpanContext ctx) {
    TraceContext result = new TraceContext();
    result.filLFromSpanContext(ctx);
    result.clockAnchor = 0L;
    return result;
  }

  private void filLFromSpanContext(SpanContext ctx) {
    id = HexUtils.hexToLong(ctx.getSpanId(), 0);
    traceIdHigh = HexUtils.hexToLong(ctx.getTraceId(), 0);
    traceIdLow = HexUtils.hexToLong(ctx.getTraceId(), 16);
    flags = ctx.getTraceFlags().asByte();

  }

  public SpanContext toOtelSpanContext(StringBuilder temporaryBuilder) {
    temporaryBuilder.setLength(0);
    HexUtils.appendLongAsHex(traceIdHigh, temporaryBuilder);
    HexUtils.appendLongAsHex(traceIdLow, temporaryBuilder);
    String traceIdStr = temporaryBuilder.toString();

    temporaryBuilder.setLength(0);
    HexUtils.appendLongAsHex(id, temporaryBuilder);
    String idStr = temporaryBuilder.toString();

    return SpanContext.create(
        traceIdStr,
        idStr,
        TraceFlags.fromByte(flags),
        TraceState.getDefault()
    );
  }

  public boolean idEquals(@Nullable TraceContext o) {
    if (o == null) {
      return false;
    }
    return id == o.id;
  }

  public static long getSpanId(byte[] serialized) {
    return ByteUtils.getLong(serialized, 16);
  }

  public void deserialize(byte[] serialized) {
    traceIdLow = ByteUtils.getLong(serialized, 0);
    traceIdHigh = ByteUtils.getLong(serialized, 8);
    id = ByteUtils.getLong(serialized, 16);
    flags = serialized[24];
    clockAnchor = ByteUtils.getLong(serialized, 25);
  }


  public boolean traceIdAndIdEquals(byte[] otherSerialized) {
    long otherTraceIdLow = ByteUtils.getLong(otherSerialized, 0);
    if (otherTraceIdLow != traceIdLow) {
      return false;
    }
    long otherTraceIdHigh = ByteUtils.getLong(otherSerialized, 8);
    if (otherTraceIdHigh != traceIdHigh) {
      return false;
    }
    long otherId = ByteUtils.getLong(otherSerialized, 16);
    return id == otherId;
  }

  public static void serialize(SpanContext ctx, long clockAnchor, byte[] buffer) {
    long id = HexUtils.hexToLong(ctx.getSpanId(), 0);
    long traceIdHigh = HexUtils.hexToLong(ctx.getTraceId(), 0);
    long traceIdLow = HexUtils.hexToLong(ctx.getTraceId(), 16);
    byte flags = ctx.getTraceFlags().asByte();
    ByteUtils.putLong(buffer, 0, traceIdLow);
    ByteUtils.putLong(buffer, 8, traceIdHigh);
    ByteUtils.putLong(buffer, 16, id);
    buffer[24] = flags;
    ByteUtils.putLong(buffer, 25, clockAnchor);
  }

  public void serialize(byte[] buffer) {
    ByteUtils.putLong(buffer, 0, traceIdLow);
    ByteUtils.putLong(buffer, 8, traceIdHigh);
    ByteUtils.putLong(buffer, 16, id);
    buffer[24] = flags;
    ByteUtils.putLong(buffer, 25, clockAnchor);
  }

  public byte[] serialize() {
    byte[] result = new byte[SERIALIZED_LENGTH];
    serialize(result);
    return result;
  }

  @Override
  public void resetState() {
    traceIdLow = 0;
    traceIdHigh = 0;
    id = 0;
    flags = 0;
    clockAnchor = 0;
  }

  public long getClockAnchor() {
    return clockAnchor;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    SpanContext otelSpanCtx = toOtelSpanContext(result);
    result.setLength(0);
    result.append(otelSpanCtx).append("(clock-anchor: ").append(clockAnchor).append(')');
    return result.toString();
  }
}
