---
title: Elastic Distribution of OpenTelemetry Java release notes
description: Release notes for Elastic Distribution of OpenTelemetry Java.
url: https://docs-v3-preview.elastic.dev/release-notes/
products:
  - Elastic Cloud Serverless
  - Elastic Distribution of OpenTelemetry SDK
  - Elastic Observability
---

# Elastic Distribution of OpenTelemetry Java release notes

Review the changes, fixes, and more in each version of Elastic Distribution of OpenTelemetry Java.
To check for security updates, go to [Security announcements for the Elastic stack](https://discuss.elastic.co/c/announcements/security-announcements/31).

# 1.5.0


### Features and enhancements

- Add support of `elastic.otel.verify.server.cert` config option to disable server certificate validation - #726
- tech preview release of central configuration support for dynamically changing instrumentation and sending, using OpAMP protocol

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.17.1](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.17.1)
- opentelemetry-sdk: [1.51.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.51.0)
- opentelemetry-semconv: [1.34.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.34.0)
- opentelemetry-java-contrib: [1.46.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.46.0)


## 1.4.1


### Fixes

- Fixed `otel.exporter.otlp.metrics.temporality.preference` config option having no effect.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.15.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.15.0).
- opentelemetry-sdk: [1.49.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.49.0).
- opentelemetry-semconv: [1.32.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.32.0).
- opentelemetry-java-contrib: [1.45.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.45.0).


## 1.4.0


### Features and enhancements

- Switched the default of `otel.exporter.otlp.metrics.temporality.preference` from `CUMULATIVE` to `DELTA` to improve dashboarding experience with Kibana. If you want to restore the previous behaviour, you can manually override `otel.exporter.otlp.metrics.temporality.preference` to `CUMULATIVE` via JVM-properties or environment variables.
- Set elastic-specific User-Agent header for OTLP exporters.
- Added support for openAI client 1.1+, drop support for older versions.
- Enabled Azure resource provider by default with `otel.resource.providers.azure.enabled` = `true`.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.15.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.15.0).
- opentelemetry-sdk: [1.49.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.49.0).
- opentelemetry-semconv: [1.32.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.32.0).
- opentelemetry-java-contrib: [1.45.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.45.0).


## 1.3.0


### Features and enhancements

- Added support for OpenAI client 0.14 to 0.31.
- Added support for OpenAI developer messages and raise minimum supported version to 0.8.0.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.13.3](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.13.3).
- opentelemetry-sdk: [1.47.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.47.0).
- opentelemetry-semconv: [1.30.0-rc.1](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.30.0-rc.1).
- opentelemetry-java-contrib: [1.44.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.44.0).


## 1.2.1


### Features and enhancements

- Added support for OpenAI client 0.13.0.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.12.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.12.0).
- opentelemetry-sdk: [1.46.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.46.0).
- opentelemetry-semconv: [1.29.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.29.0).
- opentelemetry-java-contrib: [1.42.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.42.0).


## 1.2.0


### Features and enhancements

- Added dynamically disabled instrumentation capability.
- Added disable all instrumentations option.
- Added stop-sending option.
- Added OpenAI client instrumentation.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.12.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.12.0).
- opentelemetry-sdk: [1.46.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.46.0)
- opentelemetry-semconv: [1.29.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.29.0).
- opentelemetry-java-contrib: [1.42.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.42.0).


## 1.1.0


### Fixes

- Fixed missing transitive dependencies when using universal profiling integration standalone.

This release is based on the following upstream versions:
- opentelemetry-javaagent: [2.10.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.10.0).
- opentelemetry-sdk: [1.44.1](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.44.1).
- opentelemetry-semconv: [1.28.0](https://github.com/open-telemetry/semantic-conventions-java/releases/tag/v1.28.0).
- opentelemetry-java-contrib: [1.40.0](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.40.0).


## 1.0.0

General Availability release.