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

        ReturnCode destroy(JNIEnv* jniEnv);

        void setThreadProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);
        void setProcessProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);

        jobject createThreadProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);
        jobject createProcessProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);


        ReturnCode createProfilerSocket(JNIEnv* jniEnv, jstring filepath);
        ReturnCode closeProfilerSocketIfOpen(JNIEnv* jniEnv);
        ReturnCode closeProfilerSocket(JNIEnv* jniEnv);

        jint readProfilerSocketMessage(JNIEnv* jniEnv, jobject outputBuffer);
        ReturnCode writeProfilerSocketMessage(JNIEnv* jniEnv, jbyteArray message);


        template< class... Args >
        void raiseExceptionType(JNIEnv* env, const char* exceptionClass, Args&&... messageParts) {
            jclass clazz = env->FindClass(exceptionClass);
            if(clazz != NULL) {
                std::stringstream fmt;
                ([&]{ fmt << messageParts; }(), ...);
                env->ThrowNew(clazz, fmt.str().c_str());
            }
        }

        template< class... Args >
        void raiseException(JNIEnv* env, Args&&... messageParts) {
            return raiseExceptionType(env, "java/lang/RuntimeException", messageParts...);
        }    

        template<typename Ret, class... Args >
        [[nodiscard]] Ret raiseExceptionAndReturn(JNIEnv* env, Ret retVal, Args&&... messageParts) {
            raiseExceptionType(env, "java/lang/RuntimeException", messageParts...);
            return retVal;
        }  
    }
}

#endif
