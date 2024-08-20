<!--
Goal of this doc:
Provide a complete reference of all available configuration options and where/how they can be set. (Any Elastic-specific configuration options are listed directly. General OpenTelemetry configuration options are linked.)
-->

# Configure the Elastic Distribution

Configure the Elastic Distribution of OpenTelemetry Java (EDOT Java) to send data to Elastic.

<!-- ✅ How users set configuration options -->
## Configuration methods

Set OpenTelemetry configuration options using one of the mechanisms listed in the
[OpenTelemetry agent documentation](https://opentelemetry.io/docs/zero-code/java/agent/configuration/) 
and [OpenTelemetry SDK documentation](https://opentelemetry.io/docs/languages/java/configuration/),
including:

* [Environment variables](#environment-variables)
* [System properties](#system-properties)
* [Configuration file](#configuration-file)

<!-- ✅ Order of precedence -->
Configuration options set using environment variables take precedence over system properties, and
system properties take precedence over configuration options set using file properties.

### Environment variables

<!-- ✅ What and why -->
EDOT Java can be configured using environment variables.
This is a cross-platform way to configure EDOT Java and is especially useful in containerized environments.

<!-- ✅ How -->
Define environment variables before starting the JVM:

```sh
export OTEL_SERVICE_NAME=my-service
java ...
```

### System properties

<!-- ✅ What and why -->
EDOT Java can be configured using system properties.
These configuration options can be seen by anything that can see the executed command-line.

<!-- ✅ How -->
Define system properties at the JVM start, usually on the command-line:

```sh
java -Dotel.service.name=my-service ...
```

### Configuration file

<!-- ✅ What and why -->
EDOT Java can be configured using a configuration file.

<!-- ✅ How -->
Before starting the JVM, create and populate the configuration file and specify where to find it:

```sh
echo otel.service.name=my-service > my.properties
java -Dotel.javaagent.configuration-file=my.properties ...
```

## Configuration options

Because the Elastic Distribution of OpenTelemetry Java is an extension of the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation), it supports:

* General OpenTelemetry SDK configuration options
* General OpenTelemetry Java agent configuration options
* OpenTelemetry extensions from the OpenTelemetry Java contrib repo that are included with EDOT Java
* Elastic-specific configuration options that are only available when using EDOT Java

### OpenTelemetry configuration options

EDOT Java supports all configuration options listed in the [OpenTelemetry General SDK Configuration documentation](https://opentelemetry.io/docs/languages/sdk-configuration/general/) and [OpenTelemetry Java agent documentation](https://opentelemetry.io/docs/zero-code/java/agent/configuration/) .

### Elastic distro configuration options

In addition to general OpenTelemetry configuration options, there are two kinds
of configuration options that are only available in EDOT Java:

* Additional `OTEL_` options that Elastic plans to contribute upstream to the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation), or have been contributed to the
[Java agent contrib repo](https://github.com/open-telemetry/opentelemetry-java-contrib/) and are included
in the EDOT but are not yet available in the OpenTelemetry Java agent.
* `ELASTIC_OTEL_` options that are specific to Elastic and will always live in EDOT Java (in other words, they will _not_ be added upstream).

### EDOT specific configuration options and settings

* OTEL_RESOURCE_PROVIDERS_AWS_ENABLED - enabled by default in EDOT ([disabled by default](https://opentelemetry.io/docs/zero-code/java/agent/configuration/#enable-resource-providers-that-are-disabled-by-default) in the OpenTelemetry Java agent)
* OTEL_RESOURCE_PROVIDERS_GCP_ENABLED - enabled by default in EDOT ([disabled by default](https://opentelemetry.io/docs/zero-code/java/agent/configuration/#enable-resource-providers-that-are-disabled-by-default) in the OpenTelemetry Java agent)
* OTEL_SERVICE_NAME - can be set as usual, but if not set this will be inferred when the EDOT Java agent is running in [various application servers](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/resource-providers)
* OTEL_INFERRED_SPANS_* options (see the [extension documentation](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/inferred-spans) for full details)
* OTEL_SPAN_STACK_TRACE_MIN_DURATION - in milliseconds, defaults to 5ms (see the [extension documentation](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/span-stacktrace) for full details)
* OTEL_INSTRUMENTATION_RUNTIME-TELEMETRY_EMIT-EXPERIMENTAL-TELEMETRY - Experimental runtime metrics are enabled by default. Set otel.instrumentation.runtime-telemetry.emit-experimental-telemetry to false to disable them.
* ELASTIC_OTEL_UNIVERSAL_PROFILING_INTEGRATION_* options (see the [extension documentation](https://github.com/elastic/elastic-otel-java/tree/main/universal-profiling-integration) for full details)

<!-- ✅ List auth methods -->
## Authentication methods

When sending data to Elastic, there are two ways you can authenticate: using an APM agent key or using a secret token.
Both EDOT Java and APM Server must be configured with the same secret token for the request to be accepted.

### Use an APM agent key (API key)

<!-- ✅ What and why -->
[APM agent keys](https://www.elastic.co/guide/en/observability/current/apm-api-key.html) are
used to authorize requests to an Elastic Observability endpoint.
APM agent keys are revocable, you can have more than one of them, and
you can add or remove them without restarting APM Server.

<!-- ✅ How do you authenticate using this method? -->
To create and manage APM Agent keys in Kibana:

1. Go to **APM Settings**.
1. Select the **Agent Keys** tab.

When using an APM Agent key, the `OTEL_EXPORTER_OTLP_HEADERS` is set using a
different auth schema (`ApiKey` rather than `Bearer`). For example:

<!-- ✅ Code example -->
```sh
export OTEL_EXPORTER_OTLP_ENDPOINT=https://my-deployment.apm.us-west1.gcp.cloud.es.io
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey TkpXUkx...dVZGQQ=="
```

### Use a secret token

<!-- ✅ What and why -->
[Secret tokens](https://www.elastic.co/guide/en/observability/current/apm-secret-token.html) are used to authorize requests to the APM Server.

<!-- ✅ How do you authenticate using this method? -->
You can find the values of these variables in Kibana's APM tutorial.
In Kibana:

1. Go to **Setup guides**.
1. Select **Observability**.
1. Select **Monitor my application performance**.
1. Scroll down and select the **OpenTelemetry** option.
1. The appropriate values for `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` are shown there.
  For example:

    ```sh
    export OTEL_EXPORTER_OTLP_ENDPOINT=https://my-deployment.apm.us-west1.gcp.cloud.es.io
    export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer P....l"
    ```
