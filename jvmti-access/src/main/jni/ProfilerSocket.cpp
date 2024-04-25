#include "ProfilerSocket.h"
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <fcntl.h>
#include <cstring>

namespace elastic {
    namespace jvmti_agent {

        ProfilerSocket::State::~State() {
            close(socketFd);
            if (socketFilePath != "") {
                unlink(socketFilePath.c_str());
            }
        }

        ReturnCode ProfilerSocket::State::bindToPath(JNIEnv* jniEnv, jstring filepath) {

            if(socketFilePath != "") {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Socket has already been bound to ", socketFilePath);
            }
            if(filepath == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "The provided filepath is null");
            }

            jboolean isCopy;
            sockaddr_un addr = {};
            addr.sun_family = AF_UNIX;
            const char* pathCstr = jniEnv->GetStringUTFChars(filepath, &isCopy);
            std::string pathStr(pathCstr);
            jniEnv->ReleaseStringUTFChars(filepath, pathCstr);

            size_t maxLen = sizeof(addr.sun_path) - 1;
            if (pathStr.length() > maxLen) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "The provided filepath '", pathStr,"' is too long, max allowed character count is ", maxLen);
            }
            if (pathStr.empty()) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "The provided filepath is empty");
            }
            strncpy(addr.sun_path, pathStr.c_str(), maxLen);

            if (bind(socketFd, (sockaddr*)&addr, sizeof(addr)) != 0) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not bind socket to the given filepath, error is ", errno);    
            }
            socketFilePath = pathStr;
            return ReturnCode::SUCCESS;
        }



        ReturnCode ProfilerSocket::openSocket(JNIEnv* jniEnv, jstring filepath) {
            std::lock_guard<std::mutex> guard(mutex);
            if(state != nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Profiler socket already opened!");
            }

            int socketFd = socket(PF_UNIX, SOCK_DGRAM, 0);
            if (socketFd == -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not create SOCK_DGRAM domain socket, error is ", errno);
            }
            std::unique_ptr<State> newState = std::make_unique<State>(socketFd);

            int flags = fcntl(socketFd, F_GETFL, 0);
            if (flags == -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not read fnctl flags from socket, error is ", errno);
            }

            int fnctlResult = fcntl(socketFd, F_SETFL, flags | O_NONBLOCK);
            if (fnctlResult != 0){
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not configure socket to be non-blocking, error is ", errno);                
            }

            auto bindRes = newState->bindToPath(jniEnv, filepath);
            if (bindRes != ReturnCode::SUCCESS) {
                return bindRes;
            }

            state = std::move(newState);
            return ReturnCode::SUCCESS;
        }

        ReturnCode ProfilerSocket::closeSocket(JNIEnv* jniEnv) {
            std::lock_guard<std::mutex> guard(mutex);
            if (state == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Profiler socket has not been opened yet!");
            }
            state = nullptr;
            return ReturnCode::SUCCESS;
        }

        void ProfilerSocket::destroy() {
            std::lock_guard<std::mutex> guard(mutex);
            state = nullptr;
        }

        jint ProfilerSocket::readMessage(JNIEnv* jniEnv, jobject outputBuffer) {
            std::lock_guard<std::mutex> guard(mutex);
            if (state == nullptr) {
                return raiseExceptionAndReturn(jniEnv, -1, "Profiler socket has not been opened yet!");
            }
            if (outputBuffer == nullptr) {
                return raiseExceptionAndReturn(jniEnv, -1, "No profiler socket active!");
            }

            jsize arrayLen = jniEnv->GetDirectBufferCapacity(outputBuffer);
            uint8_t* output = static_cast<uint8_t*>(jniEnv->GetDirectBufferAddress(outputBuffer));
            if (output == nullptr || arrayLen == -1) {
                return raiseExceptionAndReturn(jniEnv, -1, "Provided bytebuffer is not a direct buffer");
            }

            int n = recv(state->socketFd, output, arrayLen, 0);
            if (n == -1) {
                if(errno == EAGAIN || errno == EWOULDBLOCK) {
                    return 0; //no data to read available
                } else {
                    return raiseExceptionAndReturn(jniEnv, -1, "Failed to read from socket, error code is ", errno);
                }
            }
            return n;
        }

        ReturnCode ProfilerSocket::writeMessage(JNIEnv* jniEnv, jbyteArray message) {
            std::lock_guard<std::mutex> guard(mutex);
            if (state == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Profiler socket has not been opened yet!");
            }

            jboolean isCopy;
            jsize numBytes = jniEnv->GetArrayLength(message);
            jbyte* data = jniEnv->GetByteArrayElements(message, &isCopy);
            if (data == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not get array data");
            }
            
            sockaddr_un addr = {};
            addr.sun_family = AF_UNIX;
            strncpy(addr.sun_path, state->socketFilePath.c_str(), sizeof(addr.sun_path) - 1);

            auto result = sendto(state->socketFd, data, numBytes, MSG_DONTWAIT, (sockaddr*)&addr, sizeof(addr));
            auto errorNum = errno;

            jniEnv->ReleaseByteArrayElements(message, data, 0);
            if (result != numBytes) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not send to socket, return value is ", result," errno is ", errorNum);
            }

            return ReturnCode::SUCCESS;
        }
    }
}
