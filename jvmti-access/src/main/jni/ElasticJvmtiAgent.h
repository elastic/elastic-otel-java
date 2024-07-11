#ifndef ELASTICJVMTIAGENT_H_
#define ELASTICJVMTIAGENT_H_

#include <jvmti.h>
#include <string>

namespace elastic {
    namespace jvmti_agent {

        enum class ReturnCode {
            SUCCESS = 0,
            ERROR = -1,
            ERROR_NOT_INITIALIZED = -2,
        };

        constexpr jint toJint(ReturnCode rc) noexcept {
            return static_cast<jint>(rc);
        }

        ReturnCode init(JNIEnv* jniEnv);
        ReturnCode destroy(JNIEnv* jniEnv);

        void setThreadProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);
        void setProcessProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer);

        jobject createThreadProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);
        jobject createProcessProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity);


        ReturnCode createProfilerSocket(JNIEnv* jniEnv, jstring filepath);
        ReturnCode closeProfilerSocket(JNIEnv* jniEnv);

        jint readProfilerSocketMessage(JNIEnv* jniEnv, jobject outputBuffer);
        ReturnCode writeProfilerSocketMessage(JNIEnv* jniEnv, jbyteArray message);


        template <typename T>
        typename std::enable_if<
            false == std::is_convertible<T, std::string>::value,
            std::string>::type toStr (T&& val) {
                return std::to_string(val); 
        }
        inline std::string toStr(std::string const & val) { return val; }

        template< class... Args >
        void raiseExceptionType(JNIEnv* env, const char* exceptionClass, Args&&... messageParts) {
            jclass clazz = env->FindClass(exceptionClass);
            if(clazz != NULL) {
                std::string fmt;
                ([&]{ fmt += toStr(messageParts); }(), ...);
                env->ThrowNew(clazz, fmt.c_str());
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
