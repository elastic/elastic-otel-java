package co.elastic.otel.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Configurations {

  private static final String MESSAGE_POLLING_TRANSACTION_STRATEGY = "message_polling_transaction_strategy";
  private static final String MESSAGE_BATCH_STRATEGY = "message_batch_strategy";
  public static final String APPLICATION_PACKAGES = "application_packages";
  public static final String RECORDING = "recording";
  public static final int DEFAULT_LONG_FIELD_MAX_LENGTH = 10000;
  public static final String INSTRUMENT = "instrument";
  public static final String INSTRUMENT_ANCIENT_BYTECODE = "instrument_ancient_bytecode";
  public static final String SERVICE_NAME = "service_name";
  public static final String SERVICE_NODE_NAME = "service_node_name";
  public static final String SAMPLE_RATE = "transaction_sample_rate";
  public static final String AGENT_HOME_PLACEHOLDER = "_AGENT_HOME_";
  private static final String DEFAULT_CONFIG_FILE = AGENT_HOME_PLACEHOLDER + "/elasticapm.properties";
  public static final String CONFIG_FILE = "config_file";
  public static final String ENABLED_KEY = "enabled";
  public static final String SYSTEM_OUT = "System.out";
  static final String LOG_LEVEL_KEY = "log_level";
  static final String LOG_FILE_KEY = "log_file";
  static final String LOG_FILE_SIZE_KEY = "log_file_size";
  static final String DEFAULT_LOG_FILE = SYSTEM_OUT;

  static final String DEPRECATED_LOG_LEVEL_KEY = "logging.log_level";
  static final String DEPRECATED_LOG_FILE_KEY = "logging.log_file";
  public static final String DEFAULT_MAX_SIZE = "50mb";
  static final String LOG_FORMAT_SOUT_KEY = "log_format_sout";
  public static final String LOG_FORMAT_FILE_KEY = "log_format_file";
  static final String INITIAL_LISTENERS_LEVEL = "log4j2.StatusLogger.level";
  static final String INITIAL_STATUS_LOGGER_LEVEL = "org.apache.logging.log4j.simplelog.StatusLogger.level";
  static final String DEFAULT_LISTENER_LEVEL = "Log4jDefaultStatusLevel";

  private List<ConfigurationOption<?>> allOptions = new ArrayList<>();
  protected List<ConfigurationOption<?>> getAllOptions() {
    return allOptions;
  }

  protected <T> ConfigurationOption<T> addOption(ConfigurationOption<T> configurationOption) {
    allOptions.add(configurationOption);
    return configurationOption;
  }


  private final ConfigurationOption<Boolean> recording = addOption(ConfigurationOption.unspecifiedOption()
      .key(RECORDING).buildNotEnabled());

  private final ConfigurationOption<Boolean> enabled = addOption(ConfigurationOption.unspecifiedOption()
      .key(ENABLED_KEY).buildNotEnabled());

  private final ConfigurationOption<Boolean> instrument = addOption(ConfigurationOption.unspecifiedOption()
      .key(INSTRUMENT).buildNotEnabled());

  private final ConfigurationOption<String> serviceName = addOption(ConfigurationOption.unspecifiedOption()
      .key(SERVICE_NAME).buildNotEnabled());

  private final ConfigurationOption<String> serviceNodeName = addOption(ConfigurationOption.unspecifiedOption()
      .key(SERVICE_NODE_NAME).buildNotEnabled());

  private final ConfigurationOption<?> delayTracerStart = addOption(ConfigurationOption.unspecifiedOption()
      .key("delay_tracer_start").buildNotEnabled());

  private final ConfigurationOption<String> serviceVersion = addOption(ConfigurationOption.unspecifiedOption()
      .key("service_version").buildNotEnabled());

  private final ConfigurationOption<String> hostname = addOption(ConfigurationOption.unspecifiedOption()
      .key("hostname").buildNotEnabled());

  private final ConfigurationOption<String> environment = addOption(ConfigurationOption.unspecifiedOption()
      .key("environment").buildNotEnabled());

  private final ConfigurationOption<Double> sampleRate = addOption(ConfigurationOption.unspecifiedOption()
      .key(SAMPLE_RATE).buildNotEnabled());

  private final ConfigurationOption<Integer> transactionMaxSpans = addOption(ConfigurationOption.unspecifiedOption()
      .key("transaction_max_spans").buildNotEnabled());

  private final ConfigurationOption<Integer> longFieldMaxLength = addOption(ConfigurationOption.unspecifiedOption()
      .key("long_field_max_length").buildNotEnabled());

  private final ConfigurationOption<?> sanitizeFieldNames = addOption(ConfigurationOption.unspecifiedOption()
      .key("sanitize_field_names").buildNotEnabled());

  private final ConfigurationOption<Collection<String>> enabledInstrumentations = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_instrumentations").buildNotEnabled());

  private final ConfigurationOption<Collection<String>> disabledInstrumentations = addOption(ConfigurationOption.unspecifiedOption()
      .key("disable_instrumentations").buildNotEnabled());

  private final ConfigurationOption<Boolean> enableExperimentalInstrumentations = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_experimental_instrumentations").buildNotEnabled());

  private final ConfigurationOption<?> unnestExceptions = addOption(ConfigurationOption.unspecifiedOption()
      .key("unnest_exceptions").buildNotEnabled());

  private final ConfigurationOption<?> ignoreExceptions = addOption(ConfigurationOption.unspecifiedOption()
      .key("ignore_exceptions").buildNotEnabled());

  private final ConfigurationOption<Boolean> captureExceptionDetails = addOption(ConfigurationOption.unspecifiedOption()
      .key("capture_exception_details").buildNotEnabled());

  private final ConfigurationOption<?> captureBody = addOption(ConfigurationOption.unspecifiedOption()
      .key("capture_body").buildNotEnabled());

  private final ConfigurationOption<Boolean> captureHeaders = addOption(ConfigurationOption.unspecifiedOption()
      .key("capture_headers").buildNotEnabled());

  private final ConfigurationOption<Map<String, String>> globalLabels = addOption(ConfigurationOption.unspecifiedOption()
      .key("global_labels").buildNotEnabled());

  private final ConfigurationOption<Boolean> typePoolCache = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_type_pool_cache").buildNotEnabled());

  private final ConfigurationOption<Boolean> instrumentAncientBytecode = addOption(ConfigurationOption.unspecifiedOption()
      .key(INSTRUMENT_ANCIENT_BYTECODE).buildNotEnabled());

  private final ConfigurationOption<Boolean> warmupByteBuddy = addOption(ConfigurationOption.unspecifiedOption()
      .key("warmup_byte_buddy").buildNotEnabled());

  private final ConfigurationOption<String> bytecodeDumpPath = addOption(ConfigurationOption.unspecifiedOption()
      .key("bytecode_dump_path").buildNotEnabled());

  private final ConfigurationOption<Boolean> typeMatchingWithNamePreFilter = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_type_matching_name_pre_filtering").buildNotEnabled());

  private final ConfigurationOption<Boolean> classLoadingMatchingPreFilter = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_class_loading_pre_filtering").buildNotEnabled());

  private final ConfigurationOption<?> classesExcludedFromInstrumentation = addOption(ConfigurationOption.unspecifiedOption()
      .key("classes_excluded_from_instrumentation").buildNotEnabled());

  private final ConfigurationOption<?> defaultClassesExcludedFromInstrumentation = addOption(ConfigurationOption.unspecifiedOption()
      .key("classes_excluded_from_instrumentation_default").buildNotEnabled());

  private final ConfigurationOption<?> methodsExcludedFromInstrumentation = addOption(ConfigurationOption.unspecifiedOption()
      .key("methods_excluded_from_instrumentation").buildNotEnabled());

  private final ConfigurationOption<?> traceMethods = addOption(ConfigurationOption.unspecifiedOption()
      .key("trace_methods").buildNotEnabled());

  private final ConfigurationOption<?> traceMethodsDurationThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("trace_methods_duration_threshold").buildNotEnabled());

  private final ConfigurationOption<Boolean> centralConfig = addOption(ConfigurationOption.unspecifiedOption()
      .key("central_config").buildNotEnabled());

  private final ConfigurationOption<Boolean> breakdownMetrics = addOption(ConfigurationOption.unspecifiedOption()
      .key("breakdown_metrics").buildNotEnabled());

  private final ConfigurationOption<String> configFileLocation = addOption(ConfigurationOption.unspecifiedOption()
      .key(CONFIG_FILE).buildNotEnabled());

  private final ConfigurationOption<String> pluginsDirLocation = addOption(ConfigurationOption.unspecifiedOption()
      .key("plugins_dir").buildNotEnabled());

  private final ConfigurationOption<Boolean> useElasticTraceparentHeader = addOption(ConfigurationOption.unspecifiedOption()
      .key("use_elastic_traceparent_header").buildNotEnabled());

  private final ConfigurationOption<Boolean> disableOutgoingTraceContextHeaders = addOption(ConfigurationOption.unspecifiedOption()
      .key("disable_outgoing_tracecontext_headers").buildNotEnabled());

  private final ConfigurationOption<Integer> tracestateHeaderSizeLimit = addOption(ConfigurationOption.unspecifiedOption()
      .key("tracestate_header_size_limit").buildNotEnabled());

  private final ConfigurationOption<?> spanMinDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_min_duration").buildNotEnabled());

  private final ConfigurationOption<?> cloudProvider = addOption(ConfigurationOption.unspecifiedOption()
      .key("cloud_provider").buildNotEnabled());

  private final ConfigurationOption<?> metadataTimeoutMs = addOption(ConfigurationOption.unspecifiedOption()
      .key("metadata_timeout_ms").buildNotEnabled());

  private final ConfigurationOption<Boolean> enablePublicApiAnnotationInheritance = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_public_api_annotation_inheritance").buildNotEnabled());

  private final ConfigurationOption<?> transactionNameGroups = addOption(ConfigurationOption.unspecifiedOption()
      .key("transaction_name_groups").buildNotEnabled());

  private final ConfigurationOption<?> traceContinuationStrategy = addOption(ConfigurationOption.unspecifiedOption()
      .key("trace_continuation_strategy").buildNotEnabled());

  private final ConfigurationOption<?> activationMethod = addOption(ConfigurationOption.unspecifiedOption()
      .key("activation_method").buildNotEnabled());


  private final ConfigurationOption<?> baggateToAttach = addOption(ConfigurationOption.unspecifiedOption()
      .key("baggage_to_attach").buildNotEnabled());


  public ConfigurationOption<?> logLevel = addOption(ConfigurationOption.unspecifiedOption()
      .key(LOG_LEVEL_KEY).buildNotEnabled());

  @SuppressWarnings("unused")
  public ConfigurationOption<String> logFile = addOption(ConfigurationOption.unspecifiedOption()
      .key(LOG_FILE_KEY).buildNotEnabled());

  private final ConfigurationOption<?> logEcsReformatting = addOption(ConfigurationOption.unspecifiedOption()
      .key("log_ecs_reformatting").buildNotEnabled());

  private final ConfigurationOption<Map<String, String>> logEcsReformattingAdditionalFields = addOption(ConfigurationOption.unspecifiedOption()
      .key("log_ecs_reformatting_additional_fields").buildNotEnabled());

  private final ConfigurationOption<List<?>> logEcsFormatterAllowList = addOption(ConfigurationOption.unspecifiedOption()
      .key("log_ecs_formatter_allow_list").buildNotEnabled());

  private final ConfigurationOption<String> logEcsFormattingDestinationDir = addOption(ConfigurationOption.unspecifiedOption()
      .key("log_ecs_reformatting_dir").buildNotEnabled());

  @SuppressWarnings("unused")
  public ConfigurationOption<?> logFileSize = addOption(ConfigurationOption.unspecifiedOption()
      .key(LOG_FILE_SIZE_KEY).buildNotEnabled());

  @SuppressWarnings("unused")
  public ConfigurationOption<?> logFormatSout = addOption(ConfigurationOption.unspecifiedOption()
      .key(LOG_FORMAT_SOUT_KEY).buildNotEnabled());

  @SuppressWarnings("unused")
  public ConfigurationOption<?> logFormatFile = addOption(ConfigurationOption.unspecifiedOption()
      .key(LOG_FORMAT_FILE_KEY).buildNotEnabled());

  private final ConfigurationOption<Boolean> sendLogs = addOption(ConfigurationOption.unspecifiedOption()
      .key("log_sending").buildNotEnabled());

  private final ConfigurationOption<?> captureStatementCommands = addOption(ConfigurationOption.unspecifiedOption()
      .key("mongodb_capture_statement_commands").buildNotEnabled());

  private final ConfigurationOption<Boolean> dedotCustomMetrics = addOption(ConfigurationOption.unspecifiedOption()
      .key("dedot_custom_metrics").buildNotEnabled());

  private final ConfigurationOption<List<Double>> customMetricsHistogramBoundaries = addOption(ConfigurationOption.unspecifiedOption()
      .key("custom_metrics_histogram_boundaries").buildNotEnabled());

  private final ConfigurationOption<Integer> metricSetLimit = addOption(ConfigurationOption.unspecifiedOption()
      .key("metric_set_limit").buildNotEnabled());

  private final ConfigurationOption<Boolean> reporterHealthMetricsEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("agent_reporter_health_metrics").buildNotEnabled());

  private final ConfigurationOption<Boolean> overheadMetricsEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("agent_background_overhead_metrics").buildNotEnabled());

  private final ConfigurationOption<String> awsLambdaHandler = addOption(ConfigurationOption.unspecifiedOption()
      .key("aws_lambda_handler").buildNotEnabled());

  private final ConfigurationOption<Long> dataFlushTimeout = addOption(ConfigurationOption.unspecifiedOption()
      .key("data_flush_timeout").buildNotEnabled());

  private final ConfigurationOption<Boolean> spanCompressionEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_compression_enabled").buildNotEnabled());

  private final ConfigurationOption<?> spanCompressionExactMatchMaxDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_compression_exact_match_max_duration").buildNotEnabled());

  private final ConfigurationOption<?> spanCompressionSameKindMaxDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_compression_same_kind_max_duration").buildNotEnabled());

  private final ConfigurationOption<?> exitSpanMinDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("exit_span_min_duration").buildNotEnabled());

  private final ConfigurationOption<Boolean> circuitBreakerEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("circuit_breaker_enabled").buildNotEnabled());

  private final ConfigurationOption<?> stressMonitoringInterval = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitoring_interval").buildNotEnabled());

  private final ConfigurationOption<Double> gcStressThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitor_gc_stress_threshold").buildNotEnabled());

  private final ConfigurationOption<Double> gcReliefThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitor_gc_relief_threshold").buildNotEnabled());

  private final ConfigurationOption<?> cpuStressDurationThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitor_cpu_duration_threshold").buildNotEnabled());

  private final ConfigurationOption<Double> systemCpuStressThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitor_system_cpu_stress_threshold").buildNotEnabled());

  private final ConfigurationOption<Double> systemCpuReliefThreshold = addOption(ConfigurationOption.unspecifiedOption()
      .key("stress_monitor_system_cpu_relief_threshold").buildNotEnabled());

  private final ConfigurationOption<Collection<String>> applicationPackages = addOption(ConfigurationOption.unspecifiedOption()
      .key(APPLICATION_PACKAGES).buildNotEnabled());

  private final ConfigurationOption<Integer> stackTraceLimit = addOption(ConfigurationOption.unspecifiedOption()
      .key("stack_trace_limit").buildNotEnabled());

  private final ConfigurationOption<?> spanFramesMinDurationMs = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_frames_min_duration").buildNotEnabled());

  private final ConfigurationOption<?> spanStackTraceMinDurationMs = addOption(ConfigurationOption.unspecifiedOption()
      .key("span_stack_trace_min_duration").buildNotEnabled());

  private final ConfigurationOption<String> secretToken = addOption(ConfigurationOption.unspecifiedOption()
      .key("secret_token").buildNotEnabled());

  private final ConfigurationOption<String> apiKey = addOption(ConfigurationOption.unspecifiedOption()
      .key("api_key").buildNotEnabled());

  private final ConfigurationOption<URL> serverUrl = addOption(ConfigurationOption.unspecifiedOption()
      .key("server_url").buildNotEnabled());

  private final ConfigurationOption<List<URL>> serverUrls = addOption(ConfigurationOption.unspecifiedOption()
      .key("server_urls").buildNotEnabled());

  private final ConfigurationOption<Boolean> disableSend = addOption(ConfigurationOption.unspecifiedOption()
      .key("disable_send").buildNotEnabled());

  private final ConfigurationOption<?> serverTimeout = addOption(ConfigurationOption.unspecifiedOption()
      .key("server_timeout").buildNotEnabled());

  private final ConfigurationOption<Boolean> verifyServerCert = addOption(ConfigurationOption.unspecifiedOption()
      .key("verify_server_cert").buildNotEnabled());

  private final ConfigurationOption<Integer> maxQueueSize = addOption(ConfigurationOption.unspecifiedOption()
      .key("max_queue_size").buildNotEnabled());

  private final ConfigurationOption<Boolean> reportSynchronously = addOption(ConfigurationOption.unspecifiedOption()
      .key("report_sync").buildNotEnabled());

  private final ConfigurationOption<Boolean> includeProcessArguments = addOption(ConfigurationOption.unspecifiedOption()
      .key("include_process_args").buildNotEnabled());

  private final ConfigurationOption<?> apiRequestTime = addOption(ConfigurationOption.unspecifiedOption()
      .key("api_request_time").buildNotEnabled());

  private final ConfigurationOption<?> apiRequestSize = addOption(ConfigurationOption.unspecifiedOption()
      .key("api_request_size").buildNotEnabled());

  private final ConfigurationOption<?> metricsInterval = addOption(ConfigurationOption.unspecifiedOption()
      .key("metrics_interval").buildNotEnabled());

  private final ConfigurationOption<List<?>> disableMetrics = addOption(ConfigurationOption.unspecifiedOption()
      .key("disable_metrics").buildNotEnabled());

  private final ConfigurationOption<Boolean> enableJaxrsAnnotationInheritance = addOption(ConfigurationOption.unspecifiedOption()
      .key("enable_jaxrs_annotation_inheritance").buildNotEnabled());

  private final ConfigurationOption<Boolean> useAnnotationValueForTransactionName = addOption(ConfigurationOption.unspecifiedOption()
      .key("use_jaxrs_path_as_transaction_name").buildNotEnabled());

  private ConfigurationOption<?> captureJmxMetrics = addOption(ConfigurationOption.unspecifiedOption()
      .key("capture_jmx_metrics").buildNotEnabled());

  private final ConfigurationOption<Boolean> profilingEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_enabled").buildNotEnabled());

  private final ConfigurationOption<Boolean> profilerLoggingEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_logging_enabled").buildNotEnabled());

  private final ConfigurationOption<Boolean> backupDiagnosticFiles = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_backup_diagnostic_files").buildNotEnabled());

  private final ConfigurationOption<Integer> asyncProfilerSafeMode = addOption(ConfigurationOption.unspecifiedOption()
      .key("async_profiler_safe_mode").buildNotEnabled());

  private final ConfigurationOption<Boolean> postProcessingEnabled = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_post_processing_enabled").buildNotEnabled());

  private final ConfigurationOption<?> samplingInterval = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_sampling_interval").buildNotEnabled());

  private final ConfigurationOption<?> inferredSpansMinDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_min_duration").buildNotEnabled());

  private final ConfigurationOption<?> includedClasses = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_included_classes").buildNotEnabled());

  private final ConfigurationOption<?> excludedClasses = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_excluded_classes").buildNotEnabled());

  private final ConfigurationOption<?> profilerInterval = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_interval").buildNotEnabled());

  private final ConfigurationOption<?> profilingDuration = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_duration").buildNotEnabled());

  private final ConfigurationOption<String> profilerLibDirectory = addOption(ConfigurationOption.unspecifiedOption()
      .key("profiling_inferred_spans_lib_directory").buildNotEnabled());

  private final ConfigurationOption<?> captureContentTypes = addOption(ConfigurationOption.unspecifiedOption()
      .key("capture_body_content_types").buildNotEnabled());

  private final ConfigurationOption<?> ignoreUrls = addOption(ConfigurationOption.unspecifiedOption()
      .key("transaction_ignore_urls").buildNotEnabled());
  private final ConfigurationOption<?> ignoreUserAgents = addOption(ConfigurationOption.unspecifiedOption()
      .key("transaction_ignore_user_agents").buildNotEnabled());

  private final ConfigurationOption<Boolean> usePathAsName = addOption(ConfigurationOption.unspecifiedOption()
      .key("use_path_as_transaction_name").buildNotEnabled());

  private final ConfigurationOption<?> urlGroups = addOption(ConfigurationOption.unspecifiedOption()
      .key("url_groups").buildNotEnabled());

  private ConfigurationOption<?> messagePollingTransactionStrategy = addOption(ConfigurationOption.unspecifiedOption()
      .key(MESSAGE_POLLING_TRANSACTION_STRATEGY).buildNotEnabled());

  private ConfigurationOption<?> messageBatchStrategy = addOption(ConfigurationOption.unspecifiedOption()
      .key(MESSAGE_BATCH_STRATEGY).buildNotEnabled());

  private ConfigurationOption<Boolean> collectQueueAddress = addOption(ConfigurationOption.unspecifiedOption()
      .key("collect_queue_address").buildNotEnabled());

  private final ConfigurationOption<?> ignoreMessageQueues = addOption(ConfigurationOption.unspecifiedOption()
      .key("ignore_message_queues").buildNotEnabled());

  private final ConfigurationOption<Boolean> endMessagingTransactionOnPoll = addOption(ConfigurationOption.unspecifiedOption()
      .key("end_messaging_transaction_on_poll").buildNotEnabled());

  private final ConfigurationOption<Collection<String>> jmsListenerPackages = addOption(ConfigurationOption.unspecifiedOption()
      .key("jms_listener_packages").buildNotEnabled());

  private final ConfigurationOption<?> captureBodyUrls = addOption(ConfigurationOption.unspecifiedOption()
      .key("elasticsearch_capture_body_urls").buildNotEnabled());

}
