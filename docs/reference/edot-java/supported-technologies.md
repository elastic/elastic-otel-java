---
navigation_title: Supported Technologies
description: Overview of technologies supported by the Elastic Distribution of OpenTelemetry (EDOT) Java Agent, including JVM versions, application servers, frameworks, and LLM instrumentations.
applies_to:
  stack:
  serverless:
    observability:
  product:
    edot_java: ga
products:
  - id: cloud-serverless
  - id: observability
  - id: edot-sdk
---

# Technologies supported by the EDOT Java Agent

The EDOT Java agent is a [distribution](https://opentelemetry.io/docs/concepts/distributions/) of [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) agent. It inherits all the [supported](opentelemetry://reference/compatibility/nomenclature.md) technologies of the OpenTelemetry Java Instrumentation.

:::{note}
**Understanding auto-instrumentation scope**

Auto-instrumentation automatically captures telemetry for the frameworks and libraries listed on this page. However, it cannot instrument:

- Custom or proprietary frameworks and libraries
- Closed-source components without instrumentation support
- Application-specific business logic

If your application uses technologies not covered by auto-instrumentation, you have two options:

1. **Native OpenTelemetry support** — Some frameworks and libraries include built-in OpenTelemetry instrumentation provided by the vendor.
2. **Manual instrumentation** — Use the [OpenTelemetry API](https://opentelemetry.io/docs/languages/java/instrumentation/) to add custom spans, metrics, and logs for unsupported components.
3. **Configuration-based instrumentation** — Use the [`otel.instrumentation.methods.include`](https://opentelemetry.io/docs/zero-code/java/agent/annotations/#creating-spans-around-methods-with-otelinstrumentationmethodsinclude) option to create spans around specific methods without modifying application code or dependencies.
:::


## EDOT Collector and Elastic Stack versions

The EDOT Java agent sends data through the OpenTelemetry protocol (OTLP). While OTLP ingest works with later 8.16+ versions of the EDOT Collector, for full support use either [EDOT Collector](elastic-agent://reference/edot-collector/index.md) versions 9.x or [{{serverless-full}}](docs-content://deploy-manage/deploy/elastic-cloud/serverless.md) for OTLP ingest.

Refer to [EDOT SDKs compatibility](opentelemetry://reference/compatibility/sdks.md) for support details.

:::{note}
Ingesting data from EDOT SDKs through EDOT Collector 9.x into Elastic Stack versions 8.18+ is supported.
:::

## JVM versions

The EDOT Java agent supports Java Virtual Machine (OpenJDK, OpenJ9) versions 8+. This follows from the [OpenTelemetry supported JVMs](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#jvms-and-operating-systems).

## JVM languages

The EDOT Java agent is compatible with all JVM languages supported by the JVM version 8 and higher.

## Application servers

The EDOT Java agent supports [all the application servers documented by the OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#application-servers).

## Libraries and Frameworks instrumentations

The EDOT Java agent supports [all the libraries and frameworks documented by the OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#libraries--frameworks).

Note that [some supported technologies are deactivated by default](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#disabled-instrumentations) and need explicit configuration to be activated.

The EDOT Java agent also supports technologies listed here that are not available in the [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

Refer to the [EDOT Java agent configuration](/reference/edot-java/configuration.md#configuration-options) for defaults that might differ from the OpenTelemetry Java Instrumentation.

$$$openai-client-instrumentation$$$
