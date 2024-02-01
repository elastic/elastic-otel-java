/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyConfigurations {

  private final List<ConfigurationOption> allOptions = new ArrayList<>();

  protected List<ConfigurationOption> getAllOptions() {
    return allOptions;
  }

  protected List<ConfigurationOption> getAllImplementedOptions() {
    return allOptions.stream().filter((o) -> o.isImplemented()).collect(Collectors.toList());
  }

  protected void addUnspecifiedOption(String configurationOption) {
    allOptions.add(new ConfigurationOption(configurationOption, null));
  }

  protected void addDocumentationOption(String configurationOption, String description) {
    allOptions.add(new ConfigurationOption(configurationOption, description));
  }

  public LegacyConfigurations() {
    addAllOptions();
  }

  private void addAllOptions() {
    addDocumentationOption(
        "server_url",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-server-url[server_url] option corresponds to the OpenTelemetry https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_endpoint[OTEL_EXPORTER_OTLP_ENDPOINT]");
    addDocumentationOption(
        "server_urls",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-server-urls[server_urls] option has no equivalent OpenTelemetry option, you can only specify one endpoint (with https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_endpoint[OTEL_EXPORTER_OTLP_ENDPOINT])");
    addDocumentationOption(
        "secret_token",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-secret-token[secret_token] option corresponds to the OpenTelemetry https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_headers[OTEL_EXPORTER_OTLP_HEADERS] and would be set as, for example, OTEL_EXPORTER_OTLP_HEADERS=\"Authorization=Bearer an_apm_secret_token\" or equivalent");
    addDocumentationOption(
        "api_key",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-reporter.html#config-api-key[api_key] option corresponds to the OpenTelemetry https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_headers[OTEL_EXPORTER_OTLP_HEADERS] and would be set as, for example, OTEL_EXPORTER_OTLP_HEADERS=\"Authorization=ApiKey an_api_key\" or equivalent");
    addDocumentationOption(
        "service_name",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-name[service_name] option corresponds to the OpenTelemetry https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_service_name[OTEL_SERVICE_NAME]. The service name value can also be set using https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes[OTEL_RESOURCE_ATTRIBUTES], eg with OTEL_RESOURCE_ATTRIBUTES=service.name=myservice - if OTEL_SERVICE_NAME is set, it takes precendence over the resource attribute");
    addDocumentationOption(
        "enabled",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-enabled[enabled] option corresponds to the OpenTelemetry https://opentelemetry.io/docs/languages/java/automatic/agent-config/#suppressing-specific-auto-instrumentation[OTEL_JAVAAGENT_ENABLED]");
    addDocumentationOption(
        "service_version",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-version[service_version] option corresponds to setting the `service.version` key in https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes[OTEL_RESOURCE_ATTRIBUTES], eg with OTEL_RESOURCE_ATTRIBUTES=service.version=1.2.3");
    addDocumentationOption(
        "environment",
        "The Elastic https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-environment[environment] option corresponds to setting the `deployment.environment` key in https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/#otel_resource_attributes[OTEL_RESOURCE_ATTRIBUTES], eg with OTEL_RESOURCE_ATTRIBUTES=deployment.environment=testing");
    addUnspecifiedOption("log_level");
    addUnspecifiedOption("log_file");
    addUnspecifiedOption("log_file_size");
    addUnspecifiedOption("log_format_sout");
    addUnspecifiedOption("log_format_file");

    addUnspecifiedOption("recording");
    addUnspecifiedOption("instrument");
    addUnspecifiedOption("service_node_name");
    addUnspecifiedOption("delay_tracer_start");
    addUnspecifiedOption("service_version");
    addUnspecifiedOption("hostname");
    addUnspecifiedOption("transaction_sample_rate");
    addUnspecifiedOption("transaction_max_spans");
    addUnspecifiedOption("long_field_max_length");
    addUnspecifiedOption("sanitize_field_names");
    addUnspecifiedOption("enable_instrumentations");
    addUnspecifiedOption("disable_instrumentations");
    addUnspecifiedOption("enable_experimental_instrumentations");
    addUnspecifiedOption("unnest_exceptions");
    addUnspecifiedOption("ignore_exceptions");
    addUnspecifiedOption("capture_exception_details");
    addUnspecifiedOption("capture_body");
    addUnspecifiedOption("capture_headers");
    addUnspecifiedOption("global_labels");
    addUnspecifiedOption("enable_type_pool_cache");
    addUnspecifiedOption("instrument_ancient_bytecode");
    addUnspecifiedOption("warmup_byte_buddy");
    addUnspecifiedOption("bytecode_dump_path");
    addUnspecifiedOption("enable_type_matching_name_pre_filtering");
    addUnspecifiedOption("enable_class_loading_pre_filtering");
    addUnspecifiedOption("classes_excluded_from_instrumentation");
    addUnspecifiedOption("classes_excluded_from_instrumentation_default");
    addUnspecifiedOption("methods_excluded_from_instrumentation");
    addUnspecifiedOption("trace_methods");
    addUnspecifiedOption("trace_methods_duration_threshold");
    addUnspecifiedOption("central_config");
    addUnspecifiedOption("breakdown_metrics");
    addUnspecifiedOption("config_file");
    addUnspecifiedOption("plugins_dir");
    addUnspecifiedOption("use_elastic_traceparent_header");
    addUnspecifiedOption("disable_outgoing_tracecontext_headers");
    addUnspecifiedOption("tracestate_header_size_limit");
    addUnspecifiedOption("span_min_duration");
    addUnspecifiedOption("cloud_provider");
    addUnspecifiedOption("metadata_timeout_ms");
    addUnspecifiedOption("enable_public_api_annotation_inheritance");
    addUnspecifiedOption("transaction_name_groups");
    addUnspecifiedOption("trace_continuation_strategy");
    addUnspecifiedOption("activation_method");
    addUnspecifiedOption("baggage_to_attach");
    addUnspecifiedOption("log_ecs_reformatting");
    addUnspecifiedOption("log_ecs_reformatting_additional_fields");
    addUnspecifiedOption("log_ecs_formatter_allow_list");
    addUnspecifiedOption("log_ecs_reformatting_dir");
    addUnspecifiedOption("log_sending");
    addUnspecifiedOption("mongodb_capture_statement_commands");
    addUnspecifiedOption("dedot_custom_metrics");
    addUnspecifiedOption("custom_metrics_histogram_boundaries");
    addUnspecifiedOption("metric_set_limit");
    addUnspecifiedOption("agent_reporter_health_metrics");
    addUnspecifiedOption("agent_background_overhead_metrics");
    addUnspecifiedOption("aws_lambda_handler");
    addUnspecifiedOption("data_flush_timeout");
    addUnspecifiedOption("span_compression_enabled");
    addUnspecifiedOption("span_compression_exact_match_max_duration");
    addUnspecifiedOption("span_compression_same_kind_max_duration");
    addUnspecifiedOption("exit_span_min_duration");
    addUnspecifiedOption("circuit_breaker_enabled");
    addUnspecifiedOption("stress_monitoring_interval");
    addUnspecifiedOption("stress_monitor_gc_stress_threshold");
    addUnspecifiedOption("stress_monitor_gc_relief_threshold");
    addUnspecifiedOption("stress_monitor_cpu_duration_threshold");
    addUnspecifiedOption("stress_monitor_system_cpu_stress_threshold");
    addUnspecifiedOption("stress_monitor_system_cpu_relief_threshold");
    addUnspecifiedOption("application_packages");
    addUnspecifiedOption("stack_trace_limit");
    addUnspecifiedOption("span_frames_min_duration");
    addUnspecifiedOption("span_stack_trace_min_duration");
    addUnspecifiedOption("disable_send");
    addUnspecifiedOption("server_timeout");
    addUnspecifiedOption("verify_server_cert");
    addUnspecifiedOption("max_queue_size");
    addUnspecifiedOption("report_sync");
    addUnspecifiedOption("include_process_args");
    addUnspecifiedOption("api_request_time");
    addUnspecifiedOption("api_request_size");
    addUnspecifiedOption("metrics_interval");
    addUnspecifiedOption("disable_metrics");
    addUnspecifiedOption("enable_jaxrs_annotation_inheritance");
    addUnspecifiedOption("use_jaxrs_path_as_transaction_name");
    addUnspecifiedOption("capture_jmx_metrics");
    addUnspecifiedOption("profiling_inferred_spans_enabled");
    addUnspecifiedOption("profiling_inferred_spans_logging_enabled");
    addUnspecifiedOption("profiling_inferred_spans_backup_diagnostic_files");
    addUnspecifiedOption("async_profiler_safe_mode");
    addUnspecifiedOption("profiling_inferred_spans_post_processing_enabled");
    addUnspecifiedOption("profiling_inferred_spans_sampling_interval");
    addUnspecifiedOption("profiling_inferred_spans_min_duration");
    addUnspecifiedOption("profiling_inferred_spans_included_classes");
    addUnspecifiedOption("profiling_inferred_spans_excluded_classes");
    addUnspecifiedOption("profiling_inferred_spans_interval");
    addUnspecifiedOption("profiling_inferred_spans_duration");
    addUnspecifiedOption("profiling_inferred_spans_lib_directory");
    addUnspecifiedOption("capture_body_content_types");
    addUnspecifiedOption("transaction_ignore_urls");
    addUnspecifiedOption("transaction_ignore_user_agents");
    addUnspecifiedOption("use_path_as_transaction_name");
    addUnspecifiedOption("url_groups");
    addUnspecifiedOption("message_polling_transaction_strategy");
    addUnspecifiedOption("message_batch_strategy");
    addUnspecifiedOption("collect_queue_address");
    addUnspecifiedOption("ignore_message_queues");
    addUnspecifiedOption("end_messaging_transaction_on_poll");
    addUnspecifiedOption("jms_listener_packages");
    addUnspecifiedOption("elasticsearch_capture_body_urls");
    addUnspecifiedOption("context_propagation_only");
  }
}
