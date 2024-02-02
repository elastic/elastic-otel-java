#include "ElasticJvmtiAgent.h"
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <fcntl.h>
#include <mutex>


// These two global variables have symbol names which will be recognized by
// the elastic universal profiling host-agent. The host-agent will be able
// to read their values and the memory they point to
JNIEXPORT thread_local void* elastic_apm_profiling_correlation_tls_v1 = nullptr;
JNIEXPORT void* elastic_apm_profiling_correlation_process_storage_v1 = nullptr;

namespace elastic
{
    namespace jvmti_agent
    {

        static int profilerSocket = -1;
        static std::string profilerSocketFile;
        static std::recursive_mutex profilerSocketMutex;

        ReturnCode destroy(JNIEnv* jniEnv) {
            elastic_apm_profiling_correlation_process_storage_v1 = nullptr;
            ReturnCode code = closeProfilerSocketIfOpen(jniEnv);
            if(code != ReturnCode::SUCCESS) {
                return code;
            }

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

        ReturnCode createProfilerSocket(JNIEnv* jniEnv, jstring filepath) {
            std::lock_guard<std::recursive_mutex> guard(profilerSocketMutex);
            if(profilerSocket != -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Profiler socket already opened!");
            }
            if(filepath == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "The provided filename is null");
            }

            jboolean isCopy;
            sockaddr_un addr = {};
            addr.sun_family = AF_UNIX;
            const char* pathCstr = jniEnv->GetStringUTFChars(filepath, &isCopy);
            std::string pathStr(pathCstr);
            jniEnv->ReleaseStringUTFChars(filepath, pathCstr);

            size_t maxLen = sizeof(addr.sun_path) - 1;
            if (pathStr.length() > maxLen) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "The provided filename '", pathStr,"' is too long, max allowed character count is ", maxLen);
            }
            strncpy(addr.sun_path, pathStr.c_str(), maxLen);

            int fd = socket(PF_UNIX, SOCK_DGRAM, 0);
            if (fd == -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not create SOCK_DGRAM domain socket, error is ", errno);
            }

            int flags = fcntl(fd, F_GETFL, 0);
            if (flags == -1) {
                auto error = errno;
                close(fd);
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not read fnctl flags from socket, error is ", error);
            }

            int fnctlResult = fcntl(fd, F_SETFL, flags | O_NONBLOCK);
            if (fnctlResult != 0){
                auto error = errno;
                close(fd);
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not configure socket to be non-blocking, error is ", error);                
            }

            if (bind(fd, (sockaddr*)&addr, sizeof(addr)) != 0) {
                auto error = errno;
                close(fd);
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not bind socket to the given filepath, error is ", error);    
            }

            profilerSocket = fd;
            profilerSocketFile = pathStr;
            return ReturnCode::SUCCESS;
        }


        ReturnCode closeProfilerSocketIfOpen(JNIEnv* jniEnv) {
            std::lock_guard<std::recursive_mutex> guard(profilerSocketMutex);
            if (profilerSocket != -1) {
                return closeProfilerSocket(jniEnv);
            }
            return ReturnCode::SUCCESS;
        }

        ReturnCode closeProfilerSocket(JNIEnv* jniEnv) {
            std::lock_guard<std::recursive_mutex> guard(profilerSocketMutex);
            if (profilerSocket == -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "No profiler socket active!");
            }
            close(profilerSocket);
            unlink(profilerSocketFile.c_str());
            profilerSocket = -1;
            profilerSocketFile = "";
            return ReturnCode::SUCCESS;
        }

        jint readProfilerSocketMessage(JNIEnv* jniEnv, jobject outputBuffer) {
            std::lock_guard<std::recursive_mutex> guard(profilerSocketMutex);
            if (profilerSocket == -1) {
                return raiseExceptionAndReturn(jniEnv, -1, "No profiler socket active!");
            }
            if (outputBuffer == nullptr) {
                return raiseExceptionAndReturn(jniEnv, -1, "No profiler socket active!");
            }

            jsize arrayLen = jniEnv->GetDirectBufferCapacity(outputBuffer);
            uint8_t* output = static_cast<uint8_t*>(jniEnv->GetDirectBufferAddress(outputBuffer));
            if(output == nullptr || arrayLen == -1) {
                return raiseExceptionAndReturn(jniEnv, -1, "Provided bytebuffer is not a direct buffer");
            }

            int n = recv(profilerSocket, output, arrayLen, 0);
            if (n == -1) {
                if(errno == EAGAIN || errno == EWOULDBLOCK) {
                    return 0; //no data to read available
                } else {
                    return raiseExceptionAndReturn(jniEnv, -1, "Failed to read from socket, error code is ", errno);
                }
            }
            return n;
        }

        //ONLY FOR TESTING!
        ReturnCode writeProfilerSocketMessage(JNIEnv* jniEnv, jbyteArray message) {
            std::lock_guard<std::recursive_mutex> guard(profilerSocketMutex);
            if (profilerSocket == -1) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "No profiler socket active!");
            }

            jboolean isCopy;
            jsize numBytes = jniEnv->GetArrayLength(message);
            jbyte* data = jniEnv->GetByteArrayElements(message, &isCopy);
            if (data == nullptr) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not get array data");
            }
            
            sockaddr_un addr = {};
            addr.sun_family = AF_UNIX;
            strncpy(addr.sun_path, profilerSocketFile.c_str(), sizeof(addr.sun_path) - 1);

            auto result = sendto(profilerSocket, data, numBytes, MSG_DONTWAIT, (sockaddr*)&addr, sizeof(addr));
            auto errorNum = errno;

            jniEnv->ReleaseByteArrayElements(message, data, 0);
            if (result != numBytes) {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Could not send to socket, return value is ", result," errno is ", errorNum);
            }

            return ReturnCode::SUCCESS;
        }

    } // namespace jvmti_agent
    
} // namespace elastic
