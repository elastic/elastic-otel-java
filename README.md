# Elastic Distribution for OpenTelemetry Java

[![Snapshot status](https://badge.buildkite.com/e527255a5d6e7f5a940bc71911d8bc2be25d16702d7642c0d6.svg)](https://buildkite.com/elastic/elastic-otel-java-snapshot)
[![Release status](https://badge.buildkite.com/8bac74f475ea0d5d17ea3ea2ecf2c27a319414b97ce03dbd21.svg)](https://buildkite.com/elastic/elastic-otel-java-release)

This project is the Elastic distribution of the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

This is currently an early alpha release and should be used mostly for testing.

## Download

Latest release: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.otel/elastic-otel-javaagent?label=elastic-otel-javaagent)](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest)

Latest snapshot: [![Sonatype Nexus](https://img.shields.io/nexus/s/co.elastic.otel/elastic-otel-javaagent?server=https%3A%2F%2Foss.sonatype.org&label=elastic-otel-javaagent)](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=co.elastic.otel&a=elastic-otel-javaagent&v=LATEST)

## Run

Use the `-javaagent:` JVM argument with the path to agent jar.

```bash
java -javaagent:/path/to/agent.jar \
-jar myapp.jar
```
## Build and Test

Execute `./gradlew assemble`, the agent binary will be in `./agent/build/libs/elastic-otel-javaagent-${VERSION}.jar`
where `${VERSION}` is the current project version set in [`version.properties`](version.properties).

You can run the tests locally using `./gradlew test`. You can optionally specify the
 * Java Version to test on, e.g. `-PtestJavaVersion=8`
 * Java implementation to run on (`hotspot` or `openJ9`):  `-PtestJavaVM=openj9`

You don't need to have a corresponding JVM installed, gradle automatically will download a matching one.

## Features

### Resource attributes

The agent enables the following resource attributes providers from [opentelemetry-java-contrib](https://github.com/open-telemetry/opentelemetry-java-contrib/)
- AWS: [aws-resources](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/aws-resources)
- GCP: [gcp-resources](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/gcp-resources)
- application server service name detection: [resource-providers](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/resource-providers)

The attributes for cloud providers are captured asynchronously to prevent application startup overhead due to calling an internal metadata API.

### Inferred spans

Set `ELASTIC_OTEL_INFERRED_SPANS_ENABLED=true` to enable.

See [inferred spans](./inferred-spans/README.md) for more details

### Span stacktrace

The agent captures the stacktraces of spans to help identify code paths that triggered them.

The stacktrace is stored in the [`code.stacktrace`](https://opentelemetry.io/docs/specs/semconv/attributes-registry/code/) attribute.

The minimum span duration can be configured with `elastic.otel.span.stack.trace.min.duration` (in milliseconds, defaults to 5ms).

### Breakdown metrics

Breakdown metrics currently require a custom Elasticsearch ingest pipeline.

```
PUT _ingest/pipeline/metrics-apm.app@custom
{
  "processors": [
    {
      "script": {
        "lang": "painless",
        "source": """

if(ctx.span == null){
  ctx.span = [:];
}
if(ctx.transaction == null){
  ctx.transaction = [:];
}
if(ctx.labels != null){
  if(ctx.labels.elastic_span_type != null){
    ctx.span.type = ctx.labels.elastic_span_type;
  }
  if(ctx.labels.elastic_span_subtype != null){
    ctx.span.subtype = ctx.labels.elastic_span_subtype;
  }
  if(ctx.labels.elastic_local_root_type != null){
    ctx.transaction.type = ctx.labels.elastic_local_root_type;
  }
  if(ctx.labels.elastic_local_root_name != null){
    ctx.transaction.name = ctx.labels.elastic_local_root_name;
  }
}

if(ctx.numeric_labels != null && ctx.numeric_labels.elastic_span_self_time != null){
  def value = ctx.numeric_labels.elastic_span_self_time/1000;
  def sum = [ 'us': value];
  ctx.span.self_time =  [ 'count': 0, 'sum': sum];
}

        """
      }
    }
  ]
}
```

# License

Elastic Otel Java Distribution is licensed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

