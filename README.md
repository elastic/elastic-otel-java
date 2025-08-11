# Elastic Distribution of OpenTelemetry Java

[![Snapshot status](https://badge.buildkite.com/e527255a5d6e7f5a940bc71911d8bc2be25d16702d7642c0d6.svg)](https://buildkite.com/elastic/elastic-otel-java-snapshot)
[![Release status](https://badge.buildkite.com/8bac74f475ea0d5d17ea3ea2ecf2c27a319414b97ce03dbd21.svg)](https://buildkite.com/elastic/elastic-otel-java-release)

The Elastic Distribution of OpenTelemetry Java (EDOT Java) is a customized version of the [OpenTelemetry Java instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation). Use EDOT Java to start the OpenTelemetry SDK with your Java application, and automatically capture tracing data, performance metrics, and logs. Traces, metrics, and logs can be sent to any OpenTelemetry Protocol (OTLP) collector you choose.

With EDOT Java you have access to all the features and supported technologies of the OpenTelemetry Java instrumentation agent plus:

- additional [features](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/features), for example:
  - [Inferred spans](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/features#inferred-spans)
  - [Span stacktrace](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/features#span-stacktrace)
  - [Elastic universal profiling integration](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/features#elastic-universal-profiling-integration)
  - ...
- additional [supported technologies](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/supported-technologies)

We welcome your feedback! You can reach us by [opening a GitHub issue](https://github.com/elastic/elastic-otel-java/issues) or starting a discussion thread on the [Elastic Discuss forum](https://discuss.elastic.co/tags/c/observability/apm/58/java).

## Download

Latest release: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.otel/elastic-otel-javaagent?label=elastic-otel-javaagent)](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest)

## Run

Use the `-javaagent:` JVM argument with the path to agent jar.

```bash
java -javaagent:/path/to/agent.jar \
-jar myapp.jar
```

Using the `JAVA_TOOL_OPTIONS` environment variable can be used when modifying the JVM command line is not possible.

## Read the docs

* [EDOT Java](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/index.html)
* [EDOT Java configuration](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/configuration)
* [EDOT Java migration](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/migration) from the [Elastic APM Java agent](https://github.com/elastic/apm-agent-java)

## Build and Test

Execute `./gradlew assemble`, the agent binary will be in `./agent/build/libs/elastic-otel-javaagent-${VERSION}.jar`
where `${VERSION}` is the current project version set in [`version.properties`](version.properties).

You can run the tests locally using `./gradlew test`. You can optionally specify the
 * Java Version to test on, e.g. `-PtestJavaVersion=8`
 * Java implementation to run on (`hotspot` or `openJ9`):  `-PtestJavaVM=openj9`

You don't need to have a corresponding JVM installed, gradle automatically will download a matching one.

# License

The Elastic Distribution of OpenTelemetry Java is licensed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

