package openai.example;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;


final class Chat {

    public static void main(String[] args) {
        String chatModel = System.getenv().getOrDefault("CHAT_MODEL", "gpt-4o-mini");

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        String message = "Answer in up to 3 words: Which ocean contains Bouvet Island?";
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                    .content(message)
                    .build())
                .model(chatModel)
                .build();

        ChatCompletion chatCompletion = client.chat().completions().create(params);
        System.out.println(chatCompletion.choices().get(0).message().content().get());
    }
}
