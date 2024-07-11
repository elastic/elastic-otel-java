#include "ElasticJvmtiAgent.h"
#include "ProfilerSocket.h"

// These two global variables have symbol names which will be recognized by
// the elastic universal profiling host-agent. The host-agent will be able
// to read their values and the memory they point to
JNIEXPORT thread_local void* elastic_apm_profiling_correlation_tls_v1 = nullptr;
JNIEXPORT void* elastic_apm_profiling_correlation_process_storage_v1 = nullptr;

namespace elastic
{
    namespace jvmti_agent
    {

        static ProfilerSocket profilerSocket;
        static jvmtiEnv* jvmti;


        ReturnCode init(JNIEnv* jniEnv) {
            if(jvmti != nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "JVMTI environment is already initialized!");
            }

            JavaVM* vm;
            auto vmError = jniEnv->GetJavaVM(&vm);
            if(vmError != JNI_OK) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "jniEnv->GetJavaVM() failed, return code is ", vmError);
            }

            auto getEnvErr = vm->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_1_2);
            if(getEnvErr != JNI_OK) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "JavaVM->GetEnv() failed, return code is ", getEnvErr);
            }
            return ReturnCode::SUCCESS;
        }

        ReturnCode destroy(JNIEnv* jniEnv) {
            if(jvmti != nullptr) {
                elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
                profilerSocket.destroy();

                auto error = jvmti->DisposeEnvironment();
                jvmti = nullptr;
                if(error != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "jvmti->DisposeEnvironment() failed, return code is: ", error);
                }

                return ReturnCode::SUCCESS;
            } else {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR_NOT_INITIALIZED, "Elastic JVMTI Agent has not been initialized!");
            }
        }


        void setThreadProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer) {
            if(bytebuffer == nullptr) {
                elastic_apm_profiling_correlation_tls_v1 = nullptr;
            } else {
                elastic_apm_profiling_correlation_tls_v1 = jniEnv->GetDirectBufferAddress(bytebuffer);
            }
        }

        void setProcessProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer) {
            if(bytebuffer == nullptr) {
                elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
            } else {
                elastic_apm_profiling_correlation_process_storage_v1 = jniEnv->GetDirectBufferAddress(bytebuffer);
            }
        }

        //ONLY FOR TESTING!
        jobject createThreadProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity) {
            if(elastic_apm_profiling_correlation_tls_v1 == nullptr) {
                return nullptr;
            } else {
                return jniEnv->NewDirectByteBuffer(elastic_apm_profiling_correlation_tls_v1, capacity);
            }
        }

        //ONLY FOR TESTING!
        jobject createProcessProfilingCorrelationBufferAlias(JNIEnv* jniEnv, jlong capacity) {
            if(elastic_apm_profiling_correlation_process_storage_v1 == nullptr) {
                return nullptr;
            } else {
                return jniEnv->NewDirectByteBuffer(elastic_apm_profiling_correlation_process_storage_v1, capacity);
            }
        }

        ReturnCode createProfilerSocket(JNIEnv* jniEnv, jstring filepath) {
            return profilerSocket.openSocket(jniEnv, filepath);
        }

        ReturnCode closeProfilerSocket(JNIEnv* jniEnv) {
            return profilerSocket.closeSocket(jniEnv);
        }

        jint readProfilerSocketMessage(JNIEnv* jniEnv, jobject outputBuffer) {
            return profilerSocket.readMessage(jniEnv, outputBuffer);
        }

        //ONLY FOR TESTING!
        ReturnCode writeProfilerSocketMessage(JNIEnv* jniEnv, jbyteArray message) {
            return profilerSocket.writeMessage(jniEnv, message);
        }

    } // namespace jvmti_agent
    
} // namespace elastic
