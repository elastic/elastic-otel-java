#include "co_elastic_otel_JvmtiAccessImpl.h"


JNIEXPORT jstring JNICALL Java_co_elastic_otel_JvmtiAccessImpl_sayHello(JNIEnv* env, jclass) {
 return env->NewStringUTF("Hello from native");
}
