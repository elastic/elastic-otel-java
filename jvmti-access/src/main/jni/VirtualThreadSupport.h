#ifndef VIRTUALTHREADSUPPORT_H_
#define VIRTUALTHREADSUPPORT_H_

#include <string>
#include <mutex>
#include <optional>
#include <memory>
#include "ElasticJvmtiAgent.h"

namespace elastic {
    namespace jvmti_agent {
        

        class VirtualThreadSupport {
            private:
                std::string unsupportedReason = "Not yet initialized";
                jvmtiEnv* jvmti;
            public:

                [[nodiscard]] ReturnCode init(JNIEnv* env, jvmtiEnv* jvmti);
                [[nodiscard]] ReturnCode destroy(JNIEnv* env);

                [[nodiscard]] jstring getUnsupportedReason(JNIEnv* jniEnv);
            };
    }
}

#endif