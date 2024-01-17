#include "ElasticJvmtiAgent.h"


JNIEXPORT thread_local void* elastic_apm_profiling_correlation_tls_v1 = nullptr;

JNIEXPORT void* elastic_apm_profiling_correlation_process_storage_v1 = nullptr;

namespace elastic
{
    namespace jvmti_agent
    {

        ReturnCode destroy() {
            elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
            return ReturnCode::SUCCESS;
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

    } // namespace jvmti_agent
    
} // namespace elastic
