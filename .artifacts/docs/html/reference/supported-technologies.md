---
title: Technologies supported by the EDOT Java Agent
description: Overview of technologies supported by the Elastic Distribution of OpenTelemetry (EDOT) Java Agent, including JVM versions, application servers, frameworks, and LLM instrumentations.
url: https://docs-v3-preview.elastic.dev/reference/supported-technologies
products:
  - Elastic Cloud Serverless
  - Elastic Distribution of OpenTelemetry SDK
  - Elastic Observability
---

# Technologies supported by the EDOT Java Agent

The EDOT Java agent is a [distribution](https://opentelemetry.io/docs/concepts/distributions/) of [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) agent. It inherits all the [supported](https://docs-v3-preview.elastic.dev/elastic/opentelemetry/tree/main/reference/compatibility/nomenclature) technologies of the OpenTelemetry Java Instrumentation.

## EDOT Collector and Elastic Stack versions

The EDOT Java agent sends data through the OpenTelemetry protocol (OTLP). While OTLP ingest works with later 8.16+ versions of the EDOT Collector, for full support use either [EDOT Collector](https://docs-v3-preview.elastic.dev/elastic/elastic-agent/tree/main/reference/edot-collector) versions 9.x or [Elastic Cloud Serverless](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/deploy-manage/deploy/elastic-cloud/serverless) for OTLP ingest.
Refer to [EDOT SDKs compatibility](https://docs-v3-preview.elastic.dev/elastic/opentelemetry/tree/main/reference/compatibility/sdks) for support details.
<note>
  Ingesting data from EDOT SDKs through EDOT Collector 9.x into Elastic Stack versions 8.18+ is supported.
</note>


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
Refer to the [EDOT Java agent configuration](/reference/configuration#configuration-options) for defaults that might differ from the OpenTelemetry Java Instrumentation.

## OpenAI Client instrumentation

The minimum supported version of the OpenAI Java Client is 1.1.0. This instrumentation supports:
- Tracing for requests, including GenAI-specific attributes such as token usage.
- Opt-in logging of OpenAI request and response content payloads.
