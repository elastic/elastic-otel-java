---
title: Set up the EDOT Java Agent
description: Instructions for setting up the Elastic Distribution of OpenTelemetry (EDOT) Java in various environments, including Kubernetes and others.
url: https://docs-v3-preview.elastic.dev/reference/setup/
products:
  - Elastic Cloud Serverless
  - Elastic Distribution of OpenTelemetry SDK
  - Elastic Observability
---

# Set up the EDOT Java Agent

Learn how to set up the Elastic Distribution of OpenTelemetry (EDOT) Java in various environments, including Kubernetes and others.
<warning>
  Avoid using the Java SDK alongside any other APM agent, including Elastic APM agents. Running multiple agents in the same application process may lead to conflicting instrumentation, duplicate telemetry, or other unexpected behavior.
</warning>


## Kubernetes

For Kubernetes, use the OTel Kubernetes Operator. The Operator also manages the auto-instrumentation of Java applications. Follow the [quickstart guide](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/solutions/observability/get-started/opentelemetry/quickstart) for Kubernetes or learn more about [instrumentation details on Kubernetes for Java](https://docs-v3-preview.elastic.dev/reference/setup/k8s).

## Runtime attach

For environments where modifying the JVM arguments or configuration is not possible, or when including the EDOT Java in the application binary is necessary or preferred, use the [runtime attach](https://docs-v3-preview.elastic.dev/reference/setup/runtime-attach) setup option.

## All other environments

Follow the following Java setup guide for all other environments.

## Download the agent

You can download the latest release version of the EDOT Java agent from [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.otel/elastic-otel-javaagent?label=elastic-otel-javaagent&style=for-the-badge)](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest)

## Prerequisites

Complete the steps in the [Quickstart](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/solutions/observability/get-started/opentelemetry/quickstart) section that corresponds to your Elastic deployment model.

## Configure the Java agent

The minimal configuration to send data involves setting the values for `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables.
Set the `service.name` resource attribute explicitly with `OTEL_SERVICE_NAME` as it allows to qualify captured data and group multiple service instances together.
The following is an example that sets the `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, and `OTEL_SERVICE_NAME` environment variables:
```sh
export OTEL_EXPORTER_OTLP_ENDPOINT=https://my-deployment.apm.us-west1.gcp.cloud.es.io
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey P....l"
export OTEL_SERVICE_NAME="my-awesome-service"
```

For more advanced configuration, refer to [Configuration](https://docs-v3-preview.elastic.dev/reference/configuration).
Configuration of those environment values depends on the deployment model.

### Local EDOT Collector

When deployed locally, the EDOT Collector is accessible with `http://localhost:4318` without authentication, no further configuration is required. The `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables do not have to be set.

### Self-managed EDOT Collector

When using a self-managed EDOT Collector, set the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable to the OTLP endpoint of your self-managed EDOT Collector. If EDOT Collector requires authentication, set `OTEL_EXPORTER_OTLP_HEADERS`  to include `Authorization=ApiKey <ELASTIC_API_KEY>`.

### Elastic Managed OTLP endpoint

Follow the [Serverless quickstart guides](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/solutions/observability/get-started/opentelemetry/quickstart/serverless) to retrieve the `<ELASTIC_OTLP_ENDPOINT>` and the `<ELASTIC_API_KEY>`.
- Set `OTEL_EXPORTER_OTLP_ENDPOINT` to `<ELASTIC_OTLP_ENDPOINT>`.
- Set `OTEL_EXPORTER_OTLP_HEADERS` to include `Authorization=ApiKey <ELASTIC_API_KEY>`.


### Kubernetes

Connection to the EDOT Collector is managed by the OTel Kubernetes Operator. [Follow the Quickstart Guides](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/solutions/observability/get-started/opentelemetry/quickstart) for Kubernetes.

## Run the Java agent

Use the `-javaagent:` JVM argument with the path to agent jar. This requires to modify the JVM arguments and restart the application.
```sh
java \
-javaagent:/path/to/agent.jar \
-jar myapp.jar
```

When modifying the JVM command line arguments is not possible, use the `JAVA_TOOL_OPTIONS` environment variable to provide the `-javaagent:` argument or JVM system properties. When `JAVA_TOOL_OPTIONS` is set, all JVMs automatically use it, so make sure to limit the scope to the relevant JVMs.
Some application servers require manual steps or modification of their configuration files. Refer to [dedicated instructions](https://opentelemetry.io/docs/zero-code/java/agent/server-config/) for more details.
For applications deployed with Kubernetes, use the [OpenTelemetry Operator](https://docs-v3-preview.elastic.dev/reference/setup/k8s).

## Troubleshooting

For help with common setup issues, refer to the [EDOT Java troubleshooting guide](https://docs-v3-preview.elastic.dev/elastic/docs-content/tree/main/troubleshoot/ingest/opentelemetry/edot-sdks/java).