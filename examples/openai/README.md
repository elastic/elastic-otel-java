# OpenAI Zero-Code Instrumentation Examples

This is an example of how to instrument OpenAI calls with zero code changes,
using `instrumentation-openai-java` included in the Elastic Distribution of
OpenTelemetry Java ([EDOT Java][edot-java]).

When OpenAI examples run, they export traces, metrics and logs to an OTLP
compatible endpoint. Traces and metrics include details such as the model used
and the duration of the LLM request. In the case of chat, Logs capture the
request and the generated response. The combination of these provide a
comprehensive view of the performance and behavior of your OpenAI usage.

## Install

Download the EDOT javaagent binary.

```bash
curl -o elastic-otel-javaagent.jar -L 'https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.otel&a=elastic-otel-javaagent&v=LATEST'
```

Build the example application.

```bash
./gradlew clean assemble
```

Download shdotenv to load `.env` file when running.

```bash
curl -O -L https://github.com/ko1nksm/shdotenv/releases/download/v0.14.0/shdotenv
chmod +x ./shdotenv
```

## Configure

Copy [env.example](env.example) to `.env` and update its `OPENAI_API_KEY`.

An OTLP compatible endpoint should be listening for traces, metrics and logs on
`http://localhost:4317`. If not, update `OTEL_EXPORTER_OTLP_ENDPOINT` as well.

For example, if Elastic APM server is running locally, edit `.env` like this:
```
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8200
```

## Run

There are two examples, and they run the same way:

### Chat

[Chat](src/main/java/openai/example/Chat.java) asks the LLM a geography question and prints the response.

Run it like this:
```bash
./shdotenv java -javaagent:elastic-otel-javaagent.jar -jar build/libs/openai-example-all.jar
```

You should see something like "Atlantic Ocean" unless your LLM hallucinates!

### Embeddings


[Embeddings](src/main/java/openai/example/Embeddings.java) creates in-memory VectorDB embeddings about
Elastic products. Then, it searches for one similar to a question.

Run it like this:
```bash
./shdotenv java -classpath build/libs/openai-example-all.jar -javaagent:elastic-otel-javaagent.jar openai.example.Embeddings
```

You should see something like "Connectors can help you connect to a database",
unless your LLM hallucinates!

---

[edot-java]: https://github.com/elastic/elastic-otel-java/
