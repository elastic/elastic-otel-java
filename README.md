# Elastic Distribution of OpenTelemetry Java

[![Snapshot status](https://badge.buildkite.com/e527255a5d6e7f5a940bc71911d8bc2be25d16702d7642c0d6.svg)](https://buildkite.com/elastic/elastic-otel-java-snapshot)
[![Release status](https://badge.buildkite.com/8bac74f475ea0d5d17ea3ea2ecf2c27a319414b97ce03dbd21.svg)](https://buildkite.com/elastic/elastic-otel-java-release)

> [!WARNING]
> The Elastic Distribution of OpenTelemetry Java is not yet recommended for production use. Functionality may be changed or removed in future releases. Alpha releases are not subject to the support SLA of official GA features.
>
> We welcome your feedback! You can reach us by [opening a GitHub issue](https://github.com/elastic/elastic-otel-java/issues) or starting a discussion thread on the [Elastic Discuss forum](https://discuss.elastic.co/tags/c/observability/apm/58/java).

The Elastic Distribution of OpenTelemetry Java (EDOT Java) is a customized version of the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation). Use EDOT Java to start the OpenTelemetry SDK with your Java application, and automatically capture tracing data, performance metrics, and logs. Traces, metrics, and logs can be sent to any OpenTelemetry Protocol (OTLP) collector you choose.

With EDOT Java you have access to all the features of the OpenTelemetry Java agent plus:

* Access to SDK improvements and bug fixes contributed by the Elastic team _before_ the changes are available upstream in OpenTelemetry repositories.
* Access to optional features that can enhance OpenTelemetry data that is being sent to Elastic (for example, [inferred spans](#inferred-spans) and [span stacktrace](#span-stacktrace)).

**Ready to try out EDOT Java?** Follow the step-by-step instructions in [Get started](./docs/get-started.md).

## Download

Latest release: [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.otel/elastic-otel-javaagent?label=elastic-otel-javaagent)](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest)

Latest snapshot: [![Sonatype Nexus](https://img.shields.io/nexus/s/co.elastic.otel/elastic-otel-javaagent?server=https%3A%2F%2Foss.sonatype.org&label=elastic-otel-javaagent)](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=co.elastic.otel&a=elastic-otel-javaagent&v=LATEST)

## Run

Use the `-javaagent:` JVM argument with the path to agent jar.

```bash
java -javaagent:/path/to/agent.jar \
-jar myapp.jar
```

## Read the docs

* [Get started](./docs/get-started.md)
* [Configuration](./docs/configure.md)
* [Migrate to the Elastic Distribution of OpenTelemetry Java from the Elastic APM Java agent](./docs/migrate.md)

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

### Runtime metrics

Experimental runtime metrics are enabled by default.
Set `otel.instrumentation.runtime-telemetry.emit-experimental-telemetry` to `false` to disable them.

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

The Elastic Distribution of OpenTelemetry Java is licensed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

