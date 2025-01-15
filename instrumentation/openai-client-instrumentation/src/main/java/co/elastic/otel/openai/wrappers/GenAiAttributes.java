package co.elastic.otel.openai.wrappers;

import io.opentelemetry.api.common.AttributeKey;

import java.util.List;

public class GenAiAttributes {

    //TODO: ensure with a test that the attributes don't go out of sync with semconv

    public static final AttributeKey<String> GEN_AI_OPERATION_NAME = AttributeKey.stringKey("gen_ai.operation.name");
    public static final AttributeKey<Double> GEN_AI_REQUEST_FREQUENCY_PENALTY = AttributeKey.doubleKey("gen_ai.request.frequency_penalty");
    public static final AttributeKey<Long> GEN_AI_REQUEST_MAX_TOKENS = AttributeKey.longKey("gen_ai.request.max_tokens");
    public static final AttributeKey<String> GEN_AI_REQUEST_MODEL = AttributeKey.stringKey("gen_ai.request.model");
    public static final AttributeKey<Double> GEN_AI_REQUEST_PRESENCE_PENALTY = AttributeKey.doubleKey("gen_ai.request.presence_penalty");
    public static final AttributeKey<List<String>> GEN_AI_REQUEST_STOP_SEQUENCES = AttributeKey.stringArrayKey("gen_ai.request.stop_sequences");
    public static final AttributeKey<Double> GEN_AI_REQUEST_TEMPERATURE = AttributeKey.doubleKey("gen_ai.request.temperature");
    public static final AttributeKey<Double> GEN_AI_REQUEST_TOP_P = AttributeKey.doubleKey("gen_ai.request.top_p");
    public static final AttributeKey<List<String>> GEN_AI_RESPONSE_FINISH_REASONS = AttributeKey.stringArrayKey("gen_ai.response.finish_reasons");
    public static final AttributeKey<String> GEN_AI_RESPONSE_ID = AttributeKey.stringKey("gen_ai.response.id");
    public static final AttributeKey<String> GEN_AI_RESPONSE_MODEL = AttributeKey.stringKey("gen_ai.response.model");
    public static final AttributeKey<String> GEN_AI_SYSTEM = AttributeKey.stringKey("gen_ai.system");
    public static final AttributeKey<Long> GEN_AI_USAGE_INPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.input_tokens");
    public static final AttributeKey<Long> GEN_AI_USAGE_OUTPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.output_tokens");
    public static final AttributeKey<String> GEN_AI_TOKEN_TYPE = AttributeKey.stringKey("gen_ai.token.type");
    public static final AttributeKey<List<String>> GEN_AI_REQUEST_ENCODING_FORMATS = AttributeKey.stringArrayKey("gen_ai.request.encoding_formats");

    public static final AttributeKey<Long>
            GEN_AI_OPENAI_REQUEST_SEED = AttributeKey.longKey("gen_ai.openai.request.seed");
    public static final AttributeKey<String>
            GEN_AI_OPENAI_REQUEST_RESPONSE_FORMAT = AttributeKey.stringKey("gen_ai.openai.request.response_format");


    public static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    public static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");

    public static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    public static final String TOKEN_TYPE_INPUT = "input";
    public static final String TOKEN_TYPE_OUTPUT = "output";
}
