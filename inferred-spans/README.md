This extension is a thin wrapper around the [OpenTelemetry inferred-spans extenstions](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/inferred-spans).
It preserves backwards compatibility for the deprecated inferred spans configuration options with `ELASTIC_`/`elastic.`-prefix by mapping them to the `OTEL_`/`otel.` options.
instead of using this dependency, you should replace your usages of the deprecated config options and use the upstream dependency directly.
