package co.elastic.otel.openai.wrappers;

import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;

/**
 * Api Adapter to encapsulate breaking changes across openai-client versions.
 * If e.g. methods are renamed we add a adapter method here, so that we can provide
 * per-version implementations. These implementations have to be added to instrumentations as helpers,
 * which also ensures muzzle works effectively.
 */
public abstract class ApiAdapter {

  public static final ApiAdapter INSTANCE = lookupAdapter();

  private static ApiAdapter lookupAdapter() {
    Class<?> implClass = tryLookupClass("co.elastic.otel.openai.v0_13_0.ApiAdapterImpl");
    if (implClass == null) {
      implClass = tryLookupClass("co.elastic.otel.openai.latest.ApiAdapterImpl");
    }
    if (implClass == null) {
      throw new IllegalStateException(
          "No Adapter implementation found in instrumentation helpers!");
    }
    try {
      return (ApiAdapter) implClass.getConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to instantiate adapter", e);
    }
  }

  /**
   * Extracts the concrete message object e.g. ({@link ChatCompletionUserMessageParam})
   * from the given encapsulating {@link ChatCompletionMessageParam}.
   *
   * @param base the encapsulating param
   * @return the unboxed concrete message param type
   */
  public abstract Object extractConcreteCompletionMessageParam(ChatCompletionMessageParam base);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionToolMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionAssistantMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionSystemMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionUserMessageParam.Content content);

  /**
   * @return the text or refusal reason if either is available, otherwise null
   */
  public abstract String extractTextOrRefusal(
      ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart part);

  /**
   * @return the text if available, otherwise null
   */
  public abstract String extractText(ChatCompletionContentPart part);

  public abstract String extractType(ChatCompletionCreateParams.ResponseFormat val);

  private static Class<?> tryLookupClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

}
