package co.elastic.otel.profiler;

public class UnknownMessage implements ProfilerMessage {

  int messageType;

  public int getMessageType() {
    return messageType;
  }

  @Override
  public String toString() {
    return "UnknownMessage{" +
        "messageType=" + messageType +
        '}';
  }
}
