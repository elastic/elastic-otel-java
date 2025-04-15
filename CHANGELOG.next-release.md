* Switched the default of `otel.exporter.otlp.metrics.temporality.preference` from `CUMULATIVE` to `DELTA` to improve dashboarding experience with Kibana. If you want to restore the previous behaviour, you can manually override `otel.exporter.otlp.metrics.temporality.preference` to `CUMULATIVE` via JVM-properties or environment variables. - #583
* Set elastic-specific User-Agent header for OTLP exporters - #593
* Add support for openAI client 1.1+, drop support for older versions - #607
* Enable Azure resource provider by default with `otel.resource.providers.azure.enabled` = `true`. - #596
