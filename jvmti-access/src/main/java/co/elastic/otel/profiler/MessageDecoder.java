package co.elastic.otel.profiler;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class MessageDecoder {

  private static final Logger logger = Logger.getLogger(MessageDecoder.class.getName());

  private final TraceCorrelationMessage traceCorrelationMessage = new TraceCorrelationMessage();
  private final UnknownMessage unknownMessage = new UnknownMessage();

  public ProfilerMessage decode(ByteBuffer data) {
    int messageType = data.getShort();
    data.getShort(); //message version, not needed currently
    switch (messageType) {
      case TraceCorrelationMessage.TYPE_ID:
        return decode(traceCorrelationMessage, data);
      default:
        unknownMessage.messageType = messageType;
        return unknownMessage;
    }
  }

  private ProfilerMessage decode(TraceCorrelationMessage message, ByteBuffer data) {
    data.get(message.traceId);
    data.get(message.localRootSpanId);
    data.get(message.stackTraceId);
    message.sampleCount = data.getShort();
    return message;
  }


}
