# 1.5.0 - 28/07/2025
* Add support of `elastic.otel.verify.server.cert` config option to disable server certificate validation - #726

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.17.1](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.17.1)
* opentelemetry-sdk: [1.51.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.51.0)
* opentelemetry-semconv: [1.34.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.34.0)
* opentelemetry-java-contrib: [1.46.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.46.0)
# 1.4.1 - 16/04/2025
* Fix `otel.exporter.otlp.metrics.temporality.preference` config option having no effect. - #610

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.15.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.15.0)
* opentelemetry-sdk: [1.49.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.49.0)
* opentelemetry-semconv: [1.32.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.32.0)
* opentelemetry-java-contrib: [1.45.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.45.0)
# 1.4.0 - 15/04/2025
* Switched the default of `otel.exporter.otlp.metrics.temporality.preference` from `CUMULATIVE` to `DELTA` to improve dashboarding experience with Kibana. If you want to restore the previous behaviour, you can manually override `otel.exporter.otlp.metrics.temporality.preference` to `CUMULATIVE` via JVM-properties or environment variables. - #583
* Set elastic-specific User-Agent header for OTLP exporters - #593
* Add support for openAI client 1.1+, drop support for older versions - #607
* Enable Azure resource provider by default with `otel.resource.providers.azure.enabled` = `true`. - #596

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.15.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.15.0)
* opentelemetry-sdk: [1.49.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.49.0)
* opentelemetry-semconv: [1.32.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.32.0)
* opentelemetry-java-contrib: [1.45.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.45.0)
# 1.3.0 - 10/03/2025
* Add support for OpenAI client 0.14 to 0.31 - #531, #564
* Add support for OpenAI developer messages and raise minimum supported version to 0.8.0 - #539

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.13.3](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.13.3)
* opentelemetry-sdk: [1.47.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.47.0)
* opentelemetry-semconv: [1.30.0-rc.1](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.30.0-rc.1)
* opentelemetry-java-contrib: [1.44.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.44.0)
# 1.2.1 - 23/01/2025
* Add support for OpenAI client 0.13.0 - #514

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.12.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.12.0)
* opentelemetry-sdk: [1.46.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.46.0)
* opentelemetry-semconv: [1.29.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.29.0)
* opentelemetry-java-contrib: [1.42.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.42.0)
# 1.2.0 - 20/01/2025
* add dynamically disabled instrumentation capability - #422
* add disable all instrumentations option - #471
* add stop-sending option - #474
* Add OpenAI client instrumentation - #497

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.12.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.12.0)
* opentelemetry-sdk: [1.46.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.46.0)
* opentelemetry-semconv: [1.29.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.29.0)
* opentelemetry-java-contrib: [1.42.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.42.0)
# 1.1.0 - 21/11/2024
* Fixed missing transitive dependencies when using universal profiling integration standalone - #423

This release is based on the following upstream versions:

* opentelemetry-javaagent: [2.10.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.10.0)
* opentelemetry-sdk: [1.44.1](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.44.1)
* opentelemetry-semconv: [1.28.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.28.0)
* opentelemetry-java-contrib: [1.40.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.40.0)
# 1.0.0
GA Release
