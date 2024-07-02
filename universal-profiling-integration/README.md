OpenTelemetry extension for correlating traces with profiling data from the elastic universal profiler.

Only platform threads are supported at the moment. Virtual threads are not supported yet and will not be correlated.

## Usage

This section describes the usage of this extension outside of an agent.
Add the following dependency to your project:

```
<dependency>
    <groupId>co.elastic.otel</groupId>
    <artifactId>universal-profiling-integration</artifactId>
    <version>{latest version}</version>
</dependency>
```

### Autoconfiguration

This extension supports [autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

So if you are using an autoconfigured OpenTelemetry SDK, you'll only need to add this extension to your class path and configure it via system properties or environment variables:

| Property Name  / Environment Variable Name                                                                              | Default                                        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| elastic.otel.universal.profiling.integration.enabled <br/> ELASTIC_OTEL_UNIVERSAL_PROFILING_INTEGRATION_ENABLED         | `auto` on supported systems, `false` otherwise | Enables or disables the feature. Possible values are `true`, `false` or `auto`. On `auto` the profiling integration will be installed but remain inactive until the presence of a profiler is detected (Requires a profiling host agent 8.15 or later). This reduces the overhead in the case no profiler is there. When using `auto`, there might be a slight delay until the correlation is activated. So if your application creates spans during startup which you want correlated, you should use `true` instead.                                              |
| elastic.otel.universal.profiling.integration.socket.dir <br/> ELASTIC_OTEL_UNIVERSAL_PROFILING_INTEGRATION_SOCKET_DIR   | the value of the `java.io.tmpdir` JVM-property | The extension needs to bind a socket to a file for communicating with the universal profiling host agent. By default, this socket will be placed in the java.io.tmpdir. This configuration option can be used to change the location. Note that the total path name (including the socket) must not exceed 100 characters due to OS restrictions.                                                                                                                                                                                                                   |
| elastic.otel.universal.profiling.integration.buffer.size <br/> ELASTIC_OTEL_UNIVERSAL_PROFILING_INTEGRATION_BUFFER_SIZE | 8096                                           | The extension needs to buffer ended local-root spans for a short duration to ensure that all of its profiling data has been received. This configuration options configures the buffer size in number of spans. The higher the number of local root spans per second, the higher this buffer size should be set. The extension will log a warning if it is not capable of buffering a span due to insufficient buffer size. This will cause the span to be exported immediately instead with possibly incomplete profiling correlation data.                        |


### Manual SDK setup

If you manually set-up your `OpenTelemetrySDK`, you need to create and register an `InferredSpansProcessor` with your `TracerProvider`:

```java

Resource resource = Resource.builder()
    .put(ResourceAttributes.SERVICE_NAME, "my-service")
    .build();

SpanExporter exporter = OtlpGrpcSpanExporter.builder()
    .setEndpoint("https://<clusterid>.apm.europe-west3.gcp.cloud.es.io:443")
    .addHeader("Authorization", "Bearer <secrettoken>>")
    .build();
// Wrap exporter to ensure the correct host.id is used
exporter = new ProfilerHostIdApplyingSpanExporter(exporter);
SpanProcessor exportingProcessor = BatchSpanProcessor.builder(exporter);

UniversalProfilingProcessor processor =
    UniversalProfilingProcessor.builder(exportingProcessor, resource)
        .build();

SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
  .addSpanProcessor(processor)
  .build();
```

The `setTracerProvider(..)` call shown at the end may be omitted, in that case `GlobalOpenTelemetry` will be used for generating the inferred spans.
