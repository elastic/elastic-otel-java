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

        void destroy() {
            elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
            profilerSocket.destroy();
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
