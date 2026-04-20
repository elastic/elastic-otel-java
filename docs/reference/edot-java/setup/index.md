---
navigation_title: Setup
description: Instructions for setting up the Elastic Distribution of OpenTelemetry (EDOT) Java in various environments, including Kubernetes and others.
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

# Set up the EDOT Java Agent

Learn how to set up the {{edot}} (EDOT) Java in various environments, including Kubernetes and others.

:::{warning}
Avoid using the Java SDK alongside any other APM agent, including Elastic APM agents. Running multiple agents in the same application process may lead to conflicting instrumentation, duplicate telemetry, or other unexpected behavior.
:::

## Kubernetes

For Kubernetes, use the OTel Kubernetes Operator. The Operator also manages the auto-instrumentation of Java applications. Follow the [quickstart guide](docs-content://solutions/observability/get-started/opentelemetry/quickstart/index.md) for Kubernetes or learn more about [instrumentation details on Kubernetes for Java](/reference/edot-java/setup/k8s.md).

## Runtime attach

For environments where modifying the JVM arguments or configuration is impossible, or when including the EDOT Java in the application binary is necessary or preferred, use the [runtime attach](/reference/edot-java/setup/runtime-attach.md) setup option.

## All other environments

Follow the following Java setup guide for all other environments.

:::{agent-skill}
:url: https://github.com/elastic/agent-skills/tree/main/skills/observability/edot-java-instrument

Use this skill to instrument Java services with EDOT for tracing, metrics, and logs.
:::

## Download the agent

You can download the latest release version of the EDOT Java agent from [![Maven Central](https://img.shields.io/maven-central/v/co.elastic.otel/elastic-otel-javaagent?label=elastic-otel-javaagent&style=for-the-badge)](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest)

## Prerequisites

Complete the steps in the [Quickstart](docs-content://solutions/observability/get-started/opentelemetry/quickstart/index.md) section that corresponds to your Elastic deployment model.

##  Configure the Java agent

The minimal configuration to send data involves setting the values for `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables.

Set the `service.name` resource attribute explicitly with `OTEL_SERVICE_NAME` as it allows to qualify captured data and group multiple service instances together.

The following is an example that sets the `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, and `OTEL_SERVICE_NAME` environment variables:

```sh
export OTEL_EXPORTER_OTLP_ENDPOINT=https://my-deployment.ingest.us-west1.gcp.cloud.es.io
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey P....l"
export OTEL_SERVICE_NAME="my-awesome-service"
```

For more advanced configuration, refer to [Configuration](/reference/edot-java/configuration.md). 

Configuration of those environment values depends on the deployment model.

### Local EDOT Collector

When deployed locally, the EDOT Collector is accessible with `http://localhost:4318` without authentication, no further configuration is required. The `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables do not have to be set.

### Self-managed EDOT Collector

When using a self-managed EDOT Collector, set the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable to the OTLP endpoint of your self-managed EDOT Collector. If EDOT Collector requires authentication, set `OTEL_EXPORTER_OTLP_HEADERS`  to include `Authorization=ApiKey <ELASTIC_API_KEY>`.

### Elastic Managed OTLP endpoint

Follow the [Serverless quickstart guides](docs-content://solutions/observability/get-started/opentelemetry/quickstart/serverless/index.md) to retrieve the `<ELASTIC_OTLP_ENDPOINT>` and the `<ELASTIC_API_KEY>`.

- Set `OTEL_EXPORTER_OTLP_ENDPOINT` to `<ELASTIC_OTLP_ENDPOINT>`.
- Set `OTEL_EXPORTER_OTLP_HEADERS` to include `Authorization=ApiKey <ELASTIC_API_KEY>`.

### Kubernetes

Connection to the EDOT Collector is managed by the OTel Kubernetes Operator. [Follow the Quickstart Guides](docs-content://solutions/observability/get-started/opentelemetry/quickstart/index.md) for Kubernetes.

## Run the Java agent

Use the `-javaagent:` JVM argument with the path to agent jar. This requires to modify the JVM arguments and restart the application.

```sh
java \
-javaagent:/path/to/agent.jar \
-jar myapp.jar
```

When modifying the JVM command line arguments is not possible, use the `JAVA_TOOL_OPTIONS` environment variable to provide the `-javaagent:` argument or JVM system properties:

```sh
export JAVA_TOOL_OPTIONS="-javaagent:/path/to/agent.jar"
```

:::{note}
`JAVA_TOOL_OPTIONS` is applied to every JVM process that starts in the same environment, including helper processes, build tools, or other services sharing a host or container. In application server deployments or Kubernetes pods that run multiple JVM processes, this can result in unintended instrumentation of JVMs you did not mean to instrument.

To avoid this, prefer setting `-javaagent` directly in the server-specific startup script (refer to [Application servers](#application-servers)) rather than using `JAVA_TOOL_OPTIONS` at a system or pod level.
:::

### Application servers

Many application servers require adding the `-javaagent` argument to a server-specific configuration file rather than the process command line. The following table summarizes where to add the argument for common servers. For full details, refer to the [contrib OpenTelemetry instructions](https://opentelemetry.io/docs/zero-code/java/agent/server-config/).

| Server | Where to add `-javaagent` |
|---|---|
| **Tomcat / TomEE** | `bin/setenv.sh` (Linux) or `bin/setenv.bat` (Windows) — create the file if it doesn't exist |
| **JBoss EAP / WildFly** | `bin/standalone.conf` (Linux) or `bin/standalone.conf.bat` (Windows) — add to `JAVA_OPTS` |
| **Jetty** | `bin/jetty.sh` (`JAVA_OPTIONS`), `start.ini`, or pass directly to `java -jar start.jar` |
| **WebLogic** | `bin/startWebLogic.sh` (Linux) or `bin/startWebLogic.cmd` (Windows) — add to `JAVA_OPTIONS` |
| **Glassfish / Payara** | `asadmin create-jvm-options` command, or Admin Console → JVM Settings |
| **WebSphere Liberty** | `jvm.options` file (server-specific or global) |
| **WebSphere Traditional** | Admin Console → Servers → Application Servers → *server* → Java and Process Management → Process Definition → Java Virtual Machine → Generic JVM arguments |

For applications deployed with Kubernetes, use the [OpenTelemetry Operator](/reference/edot-java/setup/k8s.md).

## Troubleshooting

For help with common setup issues, refer to the [EDOT Java troubleshooting guide](docs-content://troubleshoot/ingest/opentelemetry/edot-sdks/java/index.md).
