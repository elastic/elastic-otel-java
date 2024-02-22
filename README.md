# Elastic OpenTelemetry Java distribution

This project is the Elastic distribution of the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

This is currently an early alpha release and should be used mostly for testing.

## Build

Execute `gradle assemble`, the agent binary will be in `./agent/build/libs/elastic-otel-javaagent-${VERSION}.jar`
where `${VERSION}` is the current project version set in `version.properties`.

## Run

Use the `-javaagent:` JVM argument with the path to agent jar.

```bash
java -javaagent:/path/to/agent.jar \
-jar myapp.jar
```

## Features

### Inferred spans

Set `ELASTIC_OTEL_INFERRED_SPANS_ENABLED=true` to enable.

See [inferred spans](./inferred-spans/README.md) for more details

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

