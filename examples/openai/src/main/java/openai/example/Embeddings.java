package openai.example;

import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;

final class Embeddings {

    public static void main(String[] args) {
        String embeddingsModel = System.getenv().getOrDefault("EMBEDDINGS_MODEL", "text-embedding-3-small");

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        var products = List.of(
                "Search: Ingest your data, and explore Elastic's machine learning and retrieval augmented generation (RAG) capabilities.",
                "Observability: Unify your logs, metrics, traces, and profiling at scale in a single platform.",
                "Security: Protect, investigate, and respond to cyber threats with AI-driven security analytics.",
                "Elasticsearch: Distributed, RESTful search and analytics.",
                "Kibana: Visualize your data. Navigate the Stack.",
                "Beats: Collect, parse, and ship in a lightweight fashion.",
                "Connectors: Connect popular databases, file systems, collaboration tools, and more.",
                "Logstash: Ingest, transform, enrich, and output."
        );

        var productEmbeddings = client.embeddings().create(
                EmbeddingCreateParams.builder()
                        .input(EmbeddingCreateParams.Input.ofArrayOfStrings(products))
                        .model(embeddingsModel)
                        .build()).data().stream().map(Embedding::embedding).toList();

        var queryEmbedding = client.embeddings().create(
                EmbeddingCreateParams.builder()
                        .input(EmbeddingCreateParams.Input.ofString("What can help me connect to a database?"))
                        .model(embeddingsModel)
                        .build()).data().get(0).embedding();

        var queryNorm = norm(queryEmbedding);

        var similarities = productEmbeddings.stream()
                .map(productEmbedding -> dot(productEmbedding, queryEmbedding) / (queryNorm * norm(productEmbedding)))
                .toList();
        double maxSimilarity = Double.MIN_VALUE;
        int maxIdx = -1;
        for (int i = 0; i < similarities.size(); i++) {
            var similarity = similarities.get(i);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                maxIdx = i;
            }
        }

        System.out.println(products.get(maxIdx));
    }

    private static double norm(List<Double> vector) {
        var sumOfSquares = vector.stream()
                .mapToDouble(Double::valueOf)
                .map(x -> x * x)
                .sum();
        return Math.sqrt(sumOfSquares);
    }

    private static double dot(List<Double> vector1, List<Double> vector2) {
        double sum = 0;
        for (int i = 0; i < vector1.size(); i++) {
            sum += vector1.get(i) * vector2.get(i);
        }
        return sum;
    }
}
