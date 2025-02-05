package co.elastic.otel.openai.latest;

import co.elastic.otel.openai.wrappers.ApiAdapter;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;

public class ApiAdapterImpl extends ApiAdapter {

  @Override
  public Object extractConcreteCompletionMessageParam(ChatCompletionMessageParam base) {
    if (base.isSystem()) {
      return base.asSystem();
    }
    if (base.isUser()) {
      return base.asUser();
    }
    if (base.isAssistant()) {
      return base.asAssistant();
    }
    if (base.isTool()) {
      return base.asTool();
    }
    throw new IllegalStateException("Unhandled message param type: " + base);
  }

  @Override
  public String asText(ChatCompletionToolMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionAssistantMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionSystemMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionUserMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String extractTextOrRefusal(
      ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart part) {
    if (part.isText()) {
      return part.asText().text();
    }
    if (part.isRefusal()) {
      return part.asRefusal().refusal();
    }
    return null;
  }

  @Override
  public String extractText(ChatCompletionContentPart part) {
    return part.isText() ? part.asText().text() : null;
  }

  @Override
  public String extractType(ChatCompletionCreateParams.ResponseFormat val) {
    if (val.isText()) {
      return val.asText()._type().toString();
    }
    if (val.isJsonObject()) {
      return val.asJsonObject()._type().toString();
    }
    if (val.isJsonSchema()) {
      return val.asJsonSchema()._type().toString();
    }
    return "";
  }

}
