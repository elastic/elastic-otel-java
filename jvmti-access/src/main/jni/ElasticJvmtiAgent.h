#ifndef ELASTICJVMTIAGENT_H_
#define ELASTICJVMTIAGENT_H_

#include <jvmti.h>
#include <sstream>

namespace elastic {
    namespace jvmti_agent {

        enum class ReturnCode {
            SUCCESS = 0,
            ERROR = -1,
        };

        constexpr jint toJint(ReturnCode rc) noexcept {
            return static_cast<jint>(rc);
        }

        ReturnCode destroy();

        void setThreadProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);
        void setProcessProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);

        jobject createThreadProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);
        jobject createProcessProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);

    }
}

#endif
