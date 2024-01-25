#include "co_elastic_otel_JvmtiAccessImpl.h"
#include "ElasticJvmtiAgent.h"

using elastic::jvmti_agent::ReturnCode;
using elastic::jvmti_agent::toJint;


JNIEXPORT jint JNICALL Java_co_elastic_otel_JvmtiAccessImpl_destroy0(JNIEnv*, jclass) {
    return toJint(elastic::jvmti_agent::destroy());
}


JNIEXPORT void JNICALL Java_co_elastic_otel_JvmtiAccessImpl_setThreadProfilingCorrelationBuffer0(JNIEnv* env, jclass, jobject bytebuffer) {
    elastic::jvmti_agent::setThreadProfilingCorrelationBuffer(env, bytebuffer);
}

JNIEXPORT void JNICALL Java_co_elastic_otel_JvmtiAccessImpl_setProcessProfilingCorrelationBuffer0(JNIEnv* env, jclass, jobject bytebuffer) {
    elastic::jvmti_agent::setProcessProfilingCorrelationBuffer(env, bytebuffer);
}

JNIEXPORT jobject JNICALL Java_co_elastic_otel_JvmtiAccessImpl_createThreadProfilingCorrelationBufferAlias(JNIEnv* env, jclass, jlong capacity) {
    return elastic::jvmti_agent::createThreadProfilingCorrelationBufferAlias(env, capacity);
}

JNIEXPORT jobject JNICALL Java_co_elastic_otel_JvmtiAccessImpl_createProcessProfilingCorrelationBufferAlias(JNIEnv* env, jclass, jlong capacity) {
    return elastic::jvmti_agent::createProcessProfilingCorrelationBufferAlias(env, capacity);
}