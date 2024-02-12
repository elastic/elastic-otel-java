#ifndef PROFILERSOCKET_H_
#define PROFILERSOCKET_H_

#include <string>
#include <mutex>
#include <optional>
#include <memory>
#include "ElasticJvmtiAgent.h"

namespace elastic {
    namespace jvmti_agent {
        

        class ProfilerSocket {
            private:
                class State {
                    public:
                    int socketFd;
                    std::string socketFilePath;

                    State(int fileDescriptor) : socketFd(fileDescriptor), socketFilePath("") {};
                    State(State&) = delete;
                    ~State();

                    [[nodiscard]] ReturnCode bindToPath(JNIEnv* jniEnv, jstring filepath);
                };

                std::mutex mutex;
                std::unique_ptr<State> state;

            public:

                [[nodiscard]] ReturnCode openSocket(JNIEnv* env, jstring filepath);
                [[nodiscard]] ReturnCode closeSocket(JNIEnv* env);
                void destroy();

                [[nodiscard]] jint readMessage(JNIEnv* jniEnv, jobject outputBuffer);
                [[nodiscard]] ReturnCode writeMessage(JNIEnv* jniEnv, jbyteArray message);
            };
    }
}

#endif