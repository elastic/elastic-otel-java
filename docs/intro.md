<!--
Goal of this doc:
Provide all the information a user needs to determine if the product is a good enough fit for their use case to merit further exploration

Assumptions we're comfortable making about the reader:
* They are familiar with Elastic
* They are familiar with OpenTelemetry
-->

# Introduction

> [!WARNING]
> The Elastic Distribution for OpenTelemetry Java is not yet recommended for production use. Functionality may be changed or removed in future releases. Alpha releases are not subject to the support SLA of official GA features.
>
> We welcome your feedback! You can reach us by [opening a GitHub issue](https://github.com/elastic/elastic-otel-java/issues) or starting a discussion thread on the [Elastic Discuss forum](https://discuss.elastic.co/tags/c/observability/apm/58/java).

<!-- ✅ Intro -->
The Elastic Distribution for OpenTelemetry Java ("the distro") is a Java package that provides:

* An easy way to instrument your application with OpenTelemetry.
* Configuration defaults for best usage.

<!--
✅ What is it?
✅ Why use it?
-->
A _distribution_ is customized version of an upstream OpenTelemetry repository with some customizations. The Elastic Distribution for OpenTelemetry Java is an extension of the [OpenTelemetry SDK for Java](https://opentelemetry.io/docs/languages/java). With the Elastic distro you have access to all the features of the OpenTelemetry SDK for Java plus:

<!--
TO DO:
These are true for the Node distro,
are then also true for the Java distro?
-->
* Access to SDK improvements and bug fixes contributed by the Elastic team _before_ the changes are available upstream in OpenTelemetry repositories.
* A single package that includes several OpenTelemetry packages as dependencies, so you only need to install and update a single package (for most use cases).
<!--
TO DO:
These are true for the .NET distro,
are they also true for the Java distro?
-->
* Elastic-specific processors that ensure optimal compatibility when exporting OpenTelemetry signal data to an Elastic backend like Elastic Observability deployment.
* Preconfigures the collection of tracing and metrics signals, applying some opinionated defaults, such as which sources are collected by default.
* Ensures that the OpenTelemetry protocol (OTLP) exporter is enabled by default.

<!--
TO DO:
Is there anything else the Elastic distro adds to the OpenTelemetry SDK for Java
that we should highlight here?
-->

> [!NOTE]
> For more details about OpenTelemetry distributions in general, visit the [OpenTelemetry documentation](https://opentelemetry.io/docs/concepts/distributions).

<!-- ✅ How to use it? -->
Use the distro to start the OpenTelemetry SDK with your Java application to automatically capture tracing data, performance metrics, and logs. Traces, metrics, and logs are sent to any OTLP collector you choose.

<!--
TO DO:
This is true for the .NET distro,
is it also true for the Java distro?
-->
Start with helpful defaults to begin collecting and exporting OpenTelemetry signals quickly. Then, further refine how you use the distro using extension methods that allow you to fully control the creation of the underlying tracer and metric providers.

After you start sending data to Elastic, use an [Elastic Observability](https://www.elastic.co/guide/en/observability/current/index.html) deployment &mdash; hosted on Elastic Cloud or on-premises &mdash; to monitor your applications, create alerts, and quickly identify root causes of service issues.

<!-- ✅ What they should do next -->
**Ready to try out the distro?** Follow the step-by-step instructions in [Get started](./get-started.md).
