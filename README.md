# Elastic OpenTelemetry agent

This is currently implemented as an agent extension, created from the example provided in [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/extension).

## Build

- extension jar: run `./gradlew build`, extension jar file will be in `build/libs/`.
- extended agent (with embedded extension): run `./gradlew extendedAgent`, extended agent will be in `build/libs/`.

## Run

- extension jar:

  ```bash
  java -javaagent:path/to/opentelemetry-javaagent.jar \
  -Dotel.javaagent.extensions=build/libs/extension-1.0-all.jar
  -jar myapp.jar
     ```

- extended agent:

  ```bash
  java -javaagent:build/lib/opentelemetry-javaagent.jar \
  -jar myapp.jar
     ```

## Setup

Breakdown metrics currently require a custom Elasticsearch ingest pipeline
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

## Project structure

- `agent`: packaged java agent
- `bootstrap`: for classes that are injected into bootstrap CL (currently empty)
- `custom`: distribution specific features
- `instrumentation`: distribution specific instrumentations (currently empty)

