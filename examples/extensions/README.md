
## Example extensions

This folder contains example extensions that can be used to modify the behavior of the OpenTelemetry and EDOT Java agents.

This complements the following upstream resources:
  - [OpenTelemetry Java Instrumentation extension examples](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/extension)
  - [OpenTelemetry Java Instrumentation extension documentation](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)

The following examples are provided:
- [./resource-attribute](./resource-attribute): Add resource attributes programmatically from custom environment variables.
- [./modify-span](./modify-span): Modify span attributes, naming and filtering programmatically on their attributes.

### Sample application

The sample application in [./test-app](./test-app) creates two kind of spans programmatically using the OpenTelemetry API.
- one `/healthcheck` span that should be filtered out by the `modify-span` extension
- one span with high cardinality name that should be normalized by the `modify-span` extension

The `resource-attribute` extension adds a custom resource attribute to all signals and is transparent to the sample application.

### Build

```shell
./gradlew assemble
```

### Run examples

```shell
export OTEL_EXPORTER_OTLP_ENDPOINT="<put your OTLP endpoint here>"
export OTEL_EXPORTER_OTLP_HEADERS="<put OTLP endpoint headers for authentication here>"
# configure extensions using environment variable
export OTEL_JAVAAGENT_EXTENSIONS="modify-span/build/libs/modify-span-all.jar,resource-attribute/build/libs/resource-attribute-all.jar"

# custom environment variable used by the resource-attribute extension
export ENVIRONMENT_NAME="test-env"

java \
-javaagent:<path to OpenTelemetry or EDOT java agent> \
-jar ./test-app/build/libs/test-app-all.jar
```
