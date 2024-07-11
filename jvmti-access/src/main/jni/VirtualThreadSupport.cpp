#include "VirtualThreadSupport.h"

namespace elastic {
    namespace jvmti_agent {
        
        ReturnCode VirtualThreadSupport::init(JNIEnv* env, jvmtiEnv* jvmti) {
            this->jvmti = jvmti;
            this->unsupportedReason = "";

            jint version;
            auto error = jvmti->GetVersionNumber(&version);
            if (error != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(env, ReturnCode::ERROR, "jvmti->GetVersionNumber() returned error code ", error);
            }

            if (version < JVMTI_VERSION_21) {
                this->unsupportedReason = "This JVM does not support JVMTI version 21+";
            }
            
            return ReturnCode::SUCCESS;
        }

        ReturnCode VirtualThreadSupport::destroy(JNIEnv*){
            return ReturnCode::SUCCESS;
        }

        jstring VirtualThreadSupport::getUnsupportedReason(JNIEnv* jniEnv){
            return jniEnv->NewStringUTF(unsupportedReason.c_str());
        }
    }
}