<!--
Goal of this doc:
The user is able to successfully see data from their Java application make it to the Elastic UI via the Elastic Distribution for OpenTelemetry Java
-->

# Get started

This guide shows you how to use the Elastic Distribution for OpenTelemetry Java (the distro) to instrument your Java application and send OpenTelemetry data to an Elastic Observability deployment.

**Already familiar with OpenTelemetry?** It's an explicit goal of this distribution to introduce _no new concepts_ outside those defined by the wider OpenTelemetry community.

**New to OpenTelemetry?** This section will guide you through the _minimal_ configuration options to get the Elastic distro set up in your application. You do _not_ need any existing experience with OpenTelemetry to set up the Elastic distro initially. If you need more control over your configuration after getting set up, you can learn more in the [OpenTelemetry documentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

<!-- ✅ What the user needs to know and/or do before they install the distro -->
## Prerequisites

Before getting started, you'll need somewhere to send the gathered OpenTelemetry data, so it can be viewed and analyzed. The distro supports sending data to any OpenTelemetry protocol (OTLP) endpoint, but this guide assumes you are sending data to an [Elastic Observability](https://www.elastic.co/observability) cloud deployment. You can use an existing one or set up a new one.

<details>
<summary><strong>Expand for setup instructions</strong></summary>

To create your first Elastic Observability deployment:

1. Sign up for a [free Elastic Cloud trial](https://cloud.elastic.co/registration) or sign into an existing account.
1. Go to <https://cloud.elastic.co/home>.
1. Click **Create deployment**.
1. When the deployment is ready, click **Open** to visit your Kibana home page (for example, `https://{DEPLOYMENT_NAME}.kb.{REGION}.cloud.es.io/app/home#/getting_started`).
</details>

<!-- ✅ How to install the distro -->
## Install

<!-- ✅ Step-by-step instructions -->
To get started with the distro:

1. Download the [latest release](https://mvnrepository.com/artifact/co.elastic.otel/elastic-otel-javaagent/latest).
1. Run the distro using the `-javaagent:` JVM argument with the path to the distro jar:

    ```bash
    java -javaagent:/path/to/agent.jar \
    -jar myapp.jar
    ```

<!-- ✅ Start-to-finish operation -->
## Send data to Elastic

After installing the distro, configure and initialize it to start
sending data to Elastic.

<!-- ✅ Provide _minimal_ configuration/setup -->
### Configure the distro

<!-- ✅ Step-by-step instructions -->
To configure the distro, at a minimum you'll need your Elastic Observability cloud deployment's OTLP endpoint and
authorization data to set the appropriate `OTLP_*` environment variables:

* `OTEL_EXPORTER_OTLP_ENDPOINT`: The full URL of the endpoint where data will be sent.
* `OTEL_EXPORTER_OTLP_HEADERS`: A comma-separated list of `key=value` pairs that will
be added to the headers of every request. This is typically this is used for authentication information.

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

<!--  ✅ What success looks like -->
## Confirm that the distro is working

To confirm that the distro has successfully connected to Elastic:

1. Go to **APM** → **Traces**.
1. You should see the name of the service to which you just added the distro.
It can take several minutes after initializing the distro for the service to show up in this list.
1. Click on the name in the list to see trace data.

    > [!NOTE]
    > There may be no trace data to visualize unless you have _used_ your application since initializing the distro.

<!-- ✅ What they should do next -->
## Next steps

* [Configure the distro](./configure.md) to send data to Elastic.
* Learn more about viewing and interpreting data in the [Observability guide](https://www.elastic.co/guide/en/observability/current/apm.html).
