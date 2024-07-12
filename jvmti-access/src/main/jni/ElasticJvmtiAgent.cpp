#include "ElasticJvmtiAgent.h"
#include "ProfilerSocket.h"
#include "VirtualThreadSupport.h"

// These two global variables have symbol names which will be recognized by
// the elastic universal profiling host-agent. The host-agent will be able
// to read their values and the memory they point to
JNIEXPORT thread_local void* elastic_apm_profiling_correlation_tls_v1 = nullptr;
JNIEXPORT void* elastic_apm_profiling_correlation_process_storage_v1 = nullptr;

namespace elastic
{
    namespace jvmti_agent
    {

        namespace {
            static ProfilerSocket profilerSocket;
            static VirtualThreadSupport virtualThreads;
            static jvmtiEnv* jvmti;
        }


        ReturnCode init(JNIEnv* jniEnv) {
            if(jvmti != nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "JVMTI environment is already initialized!");
            }
            JavaVM* vm;
            auto vmError = jniEnv->GetJavaVM(&vm);
            if(vmError != JNI_OK) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "jniEnv->GetJavaVM() failed, return code is ", vmError);
            }

            auto getEnvErr = vm->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_21);
            if(getEnvErr == JNI_EVERSION) {
                getEnvErr = vm->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_1_2);
            }
            if(getEnvErr != JNI_OK) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "JavaVM->GetEnv() failed, return code is ", getEnvErr);
            }
            return virtualThreads.init(jniEnv, jvmti);
        }

        ReturnCode destroy(JNIEnv* jniEnv) {
            if(jvmti != nullptr) {
                elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
                profilerSocket.destroy();

                ReturnCode vterror = virtualThreads.destroy(jniEnv);
                if (vterror != ReturnCode::SUCCESS) {
                    return vterror;
                }

                auto error = jvmti->DisposeEnvironment();
                jvmti = nullptr;
                if (error != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "jvmti->DisposeEnvironment() failed, return code is: ", error);
                }

                return ReturnCode::SUCCESS;
            } else {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR_NOT_INITIALIZED, "JVMTI environment has not been initialized yet!");
            }
        }

        void onVirtualThreadMount(JNIEnv*, jthread) {
            void* address = nullptr;
            jvmti->GetThreadLocalStorage(nullptr, &address);
            elastic_apm_profiling_correlation_tls_v1 = address;
        }

        void onVirtualThreadUnmount(JNIEnv*, jthread){
            elastic_apm_profiling_correlation_tls_v1 = nullptr;
        }

        ReturnCode setVirtualThreadProfilingCorrelationEnabled(JNIEnv* jniEnv, jboolean enable) {
            if(jvmti == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR_NOT_INITIALIZED, "JVMTI environment has not been initialized yet!");
            }
            return virtualThreads.setMountCallbacksEnabled(jniEnv, enable == JNI_TRUE);
        }

        ReturnCode setThreadProfilingCorrelationBuffer(JNIEnv* jniEnv, jobject bytebuffer) {
            if(jvmti == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR_NOT_INITIALIZED, "JVMTI environment has not been initialized yet!");
            }
            void* address = nullptr;
            if (bytebuffer != nullptr) {
                address = jniEnv->GetDirectBufferAddress(bytebuffer);
            }
            auto error = jvmti->SetThreadLocalStorage(nullptr, address);
            if (error != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR_NOT_INITIALIZED, "jvmti->SetThreadLocalStorage() returned error code ", error);
            }
            elastic_apm_profiling_correlation_tls_v1 = address;
            return ReturnCode::SUCCESS;
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
