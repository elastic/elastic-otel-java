
### Example extensions

This folder contains example extensions that can be used to modify the behavior of the OpenTelemetry and EDOT Java agents.

This complements the following upstream resources:
  - [OpenTelemetry Java Instrumentation extension examples](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/extension)
  - [OpenTelemetry Java Instrumentation extension documentation](https://opentelemetry.io/docs/zero-code/java/agent/extensions/)

The following examples are provided:
- [./resource-attribute](./resource-attribute): Add resource attributes programmatically from custom environment variables.
- [./modify-span](./modify-span): Modify span attributes, naming and filtering programmatically on their attributes.
