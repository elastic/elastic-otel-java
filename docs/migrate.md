# Migrate to the Elastic distribution

<!--
This file is auto generated. Please only make changes in `migrate.md.ftl`
-->

This documentation describes how to update applications using the [Elastic APM Java agent](https://www.elastic.co/guide/en/apm/agent/java/current/index.html) to use the Elastic Distribution of OpenTelemetry Java (EDOT Java).

Start by installing EDOT Java following the steps outlined in [Get started](./get-started.md). Then update existing APM Java agent configuration options in your application with the equivalent [OpenTelemetry SDK configuration variables](https://opentelemetry.io/docs/languages/sdk-configuration/general/) (listed below).

## Option reference

This is a list of all APM Java agent configuration options grouped by their category.
Select one of the following for more information.

* [server_url](#server_url)
* [server_urls](#server_urls)
* [secret_token](#secret_token)
* [api_key](#api_key)
* [service_name](#service_name)
* [enabled](#enabled)
* [service_version](#service_version)
* [environment](#environment)
* [global_labels](#global_labels)

## Elastic to OpenTelemetry mapping

### `server_url`

The Elastic [`server_url`](https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-server-url) option corresponds to the OpenTelemetry [`OTEL_EXPORTER_OTLP_ENDPOINT`](https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_endpoint) option.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `server_urls`

The Elastic [`server_urls`](https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-server-urls) option has no equivalent OpenTelemetry option - you can only specify one endpoint. Use [OTEL_EXPORTER_OTLP_ENDPOINT](https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_endpoint) instead.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `secret_token`

The Elastic [`secret_token`](https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-secret-token) option corresponds to the OpenTelemetry [OTEL_EXPORTER_OTLP_HEADERS](https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_headers) option. For example: `OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer an_apm_secret_token"`.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `api_key`

The Elastic [`api_key`](https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-api-key) option corresponds to the OpenTelemetry [OTEL_EXPORTER_OTLP_HEADERS](https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_headers) option. For example:`OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey an_api_key"`.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `service_name`

The Elastic [`service_name`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-name) option corresponds to the OpenTelemetry [OTEL_SERVICE_NAME](https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_service_name) option. The service name value can also be set using [OTEL_RESOURCE_ATTRIBUTES](https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes). For example: `OTEL_RESOURCE_ATTRIBUTES=service.name=myservice`. If `OTEL_SERVICE_NAME` is set, it takes precedence over the resource attribute.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `enabled`

The Elastic [`enabled`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-enabled) option corresponds to the OpenTelemetry [OTEL_JAVAAGENT_ENABLED](https://opentelemetry.io/docs/languages/java/automatic/agent-config/#suppressing-specific-auto-instrumentation) option.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `service_version`

The Elastic [`service_version`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-version) option corresponds to setting the `service.version` key in [OTEL_RESOURCE_ATTRIBUTES](https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes). For example: `OTEL_RESOURCE_ATTRIBUTES=service.version=1.2.3`.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `environment`

The Elastic [`environment`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-environment) option corresponds to setting the `deployment.environment` key in [OTEL_RESOURCE_ATTRIBUTES](https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes). For example: `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=testing`.

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

### `global_labels`

The Elastic [`global_labels`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-global-labels) option corresponds to adding `key=value` comma separated pairs in [OTEL_RESOURCE_ATTRIBUTES](https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes). For example: `OTEL_RESOURCE_ATTRIBUTES=alice=first,bob=second`. Such labels will result in labels.key=value attributes on the server, e.g. labels.alice=first

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->



<!-- Elastic to OpenTelemetry mapping -->
