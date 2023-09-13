package co.elastic.otel;

import io.opentelemetry.api.common.AttributeKey;

public interface ElasticAttributes {
    AttributeKey<Long> SELF_TIME_ATTRIBUTE = AttributeKey.longKey("elastic.span.self_time");
    AttributeKey<String> LOCAL_ROOT_ID = AttributeKey.stringKey("elastic.span.local_root.id");
    AttributeKey<String> LOCAL_ROOT_NAME = AttributeKey.stringKey("elastic.local_root.name");
    AttributeKey<String> LOCAL_ROOT_TYPE = AttributeKey.stringKey("elastic.local_root.type");
    AttributeKey<Boolean> IS_LOCAL_ROOT = AttributeKey.booleanKey("elastic.span.is_local_root");
    AttributeKey<String> ELASTIC_SPAN_TYPE = AttributeKey.stringKey("elastic.span.type");
    AttributeKey<String> ELASTIC_SPAN_SUBTYPE = AttributeKey.stringKey("elastic.span.subtype");

}
