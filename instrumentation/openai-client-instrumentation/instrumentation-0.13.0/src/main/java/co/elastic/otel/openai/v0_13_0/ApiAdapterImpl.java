package co.elastic.otel.openai.v0_13_0;

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
    if (base.isChatCompletionSystemMessageParam()) {
      return base.asChatCompletionSystemMessageParam();
    }
    if (base.isChatCompletionUserMessageParam()) {
      return base.asChatCompletionUserMessageParam();
    }
    if (base.isChatCompletionAssistantMessageParam()) {
      return base.asChatCompletionAssistantMessageParam();
    }
    if (base.isChatCompletionToolMessageParam()) {
      return base.asChatCompletionToolMessageParam();
    }
    throw new IllegalStateException("Unhandled message param type: " + base);
  }

  @Override
  public String extractText(ChatCompletionContentPart part) {
    if (part.isChatCompletionContentPartText()) {
      return part.asChatCompletionContentPartText().text();
    }
    return null;
  }

  @Override
  public String extractType(ChatCompletionCreateParams.ResponseFormat val) {
    if (val.isResponseFormatText()) {
      return val.asResponseFormatText()._type().toString();
    } else if (val.isResponseFormatJsonObject()) {
      return val.asResponseFormatJsonObject()._type().toString();
    } else if (val.isResponseFormatJsonSchema()) {
      return val.asResponseFormatJsonSchema()._type().toString();
    }
    return "";
  }

  @Override
  public String extractTextOrRefusal(
      ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart part) {
    if (part.isChatCompletionContentPartText()) {
      return part.asChatCompletionContentPartText().text();
    }
    if (part.isChatCompletionContentPartRefusal()) {
      return part.asChatCompletionContentPartRefusal().refusal();
    }
    return null;
  }

  @Override
  public String asText(ChatCompletionUserMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    }
    return "";
  }

  @Override
  public String asText(ChatCompletionSystemMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    }
    return "";
  }

  @Override
  public String asText(ChatCompletionAssistantMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    }
    return "";
  }

  @Override
  public String asText(ChatCompletionToolMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    }
    return "";
  }
}
