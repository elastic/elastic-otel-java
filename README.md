# Disclamer

This is currently a work-in-progress project.

# Elastic OpenTelemetry Java agent

This project is the Elastic distribution of OpenTelemetry Java agent.

## Build

Execute `gradle assemble`, the agent binary will be in `./agent/build/libs/elastic-otel-javaagent-${VERSION}.jar`
where `${VERSION}` is the current project version.

### Project structure

- `agent`: packaged java agent
- `bootstrap`: for classes that are injected into bootstrap CL (currently empty)
- `custom`: distribution specific code
- `instrumentation`: distribution specific instrumentations (currently empty)
- `smoke-tests`: smoke tests

## Run

Use the `-javaagent:` JVM argument with the path to agent jar.

  ```bash
  java -javaagent:/path/to/agent.jar \
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

# Extending your own OpenTelemetry SDK
TODO


