#include "VirtualThreadSupport.h"
#include <cstring>

namespace elastic {
    namespace jvmti_agent {

        namespace {

            bool isExpectedMountEvent(jvmtiExtensionEventInfo& eventInfo) {
                if(strcmp(eventInfo.id, "com.sun.hotspot.events.VirtualThreadMount") != 0) {
                    return false;
                }
                if(eventInfo.param_count != 2) {
                    return false;
                }
                if(eventInfo.params[0].base_type != JVMTI_TYPE_JNIENV) {
                    return false;
                }
                if(eventInfo.params[0].kind != JVMTI_KIND_IN_PTR) {
                    return false;
                }
                if(eventInfo.params[0].null_ok) {
                    return false;
                }
                if(eventInfo.params[1].base_type != JVMTI_TYPE_JTHREAD) {
                    return false;
                }
                if(eventInfo.params[1].kind != JVMTI_KIND_IN) {
                    return false;
                }
                if(eventInfo.params[1].null_ok) {
                    return false;
                }
                return true;
            }

            bool isExpectedUnmountEvent(jvmtiExtensionEventInfo& eventInfo) {
                if(strcmp(eventInfo.id, "com.sun.hotspot.events.VirtualThreadUnmount") != 0) {
                    return false;
                }
                if(eventInfo.param_count != 2) {
                    return false;
                }
                if(eventInfo.params[0].base_type != JVMTI_TYPE_JNIENV) {
                    return false;
                }
                if(eventInfo.params[0].kind != JVMTI_KIND_IN_PTR) {
                    return false;
                }
                if(eventInfo.params[0].null_ok) {
                    return false;
                }
                if(eventInfo.params[1].base_type != JVMTI_TYPE_JTHREAD) {
                    return false;
                }
                if(eventInfo.params[1].kind != JVMTI_KIND_IN) {
                    return false;
                }
                if(eventInfo.params[1].null_ok) {
                    return false;
                }
                return true;
            }

            void JNICALL vtMountHandler(jvmtiEnv* jvmtiEnv, ...) {
                va_list args;
                va_start(args, jvmtiEnv);
                JNIEnv* jniEnv = va_arg(args, JNIEnv*);
                jthread argThread = va_arg(args, jthread);
                va_end(args);

                jvmti_agent::onVirtualThreadMount(jniEnv, argThread);
            }

            void JNICALL vtUnmountHandler(jvmtiEnv* jvmtiEnv, ...) {
                va_list args;
                va_start(args, jvmtiEnv);
                JNIEnv* jniEnv = va_arg(args, JNIEnv*);
                jthread argThread = va_arg(args, jthread);
                va_end(args);

                jvmti_agent::onVirtualThreadUnmount(jniEnv, argThread);
            }
        }
        
        ReturnCode VirtualThreadSupport::init(JNIEnv* env, jvmtiEnv* jvmti) {
            this->jvmti = jvmti;
            this->unsupportedReason = "Not yet initialized";

            jint version;
            auto error = jvmti->GetVersionNumber(&version);
            if (error != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(env, ReturnCode::ERROR, "jvmti->GetVersionNumber() returned error code ", error);
            }

            if (version < JVMTI_VERSION_21) {
                this->unsupportedReason = "This JVM does not support JVMTI version 21+";
                return ReturnCode::SUCCESS;
            }

            jvmtiCapabilities supportedCapabilities;
            auto supErr =jvmti->GetPotentialCapabilities(&supportedCapabilities);
            if(supErr != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(env, ReturnCode::ERROR, "Failed to get JVMTI supported capabilities", supErr);
            }

            bool virtualThreadsCapabilitySupported = supportedCapabilities.can_support_virtual_threads != 0;
            if (!virtualThreadsCapabilitySupported) {
                this->unsupportedReason = "The JVMTI environment can not support the can_support_virtual_threads capability";
                return ReturnCode::SUCCESS;                
            }

            jvmtiCapabilities caps = {};
            caps.can_support_virtual_threads = 1;
            auto capErr = jvmti->AddCapabilities(&caps);
            if(capErr != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(env, ReturnCode::ERROR, "Failed to add virtual threads capability", capErr);
            }

            jint extensionCount;
            jvmtiExtensionEventInfo* extensionInfos;
            error = jvmti->GetExtensionEvents(&extensionCount, &extensionInfos);
            if (error != JVMTI_ERROR_NONE) {
                return raiseExceptionAndReturn(env, ReturnCode::ERROR, "jvmti->GetExtensionEvents() returned error code ", error);
            }

            mountEventIdx = -1;
            unmountEventIdx = -1;
            for(int i=0; i<extensionCount; i++) {
                if(isExpectedMountEvent(extensionInfos[i])) {
                    mountEventIdx = extensionInfos[i].extension_event_index;
                }
                if(isExpectedUnmountEvent(extensionInfos[i])) {
                    unmountEventIdx = extensionInfos[i].extension_event_index;
                }
            }
            jvmti->Deallocate((unsigned char*) extensionInfos);

            if (mountEventIdx == -1) {
                this->unsupportedReason = "This JVM does not support the JVMTI com.sun.hotspot.events.VirtualThreadMount event";
                return ReturnCode::SUCCESS;
            }
            if (unmountEventIdx == -1) {
                this->unsupportedReason = "This JVM does not support the JVMTI com.sun.hotspot.events.VirtualThreadUnmount event";
                return ReturnCode::SUCCESS;
            }

            this->unsupportedReason = "";
            return ReturnCode::SUCCESS;
        }

        ReturnCode VirtualThreadSupport::destroy(JNIEnv* jni){
            if (this->eventsEnabled) {
                return this->setMountCallbacksEnabled(jni, false);
            }
            return ReturnCode::SUCCESS;
        }

        ReturnCode VirtualThreadSupport::setMountCallbacksEnabled(JNIEnv* jniEnv, bool enabled) {
            if (eventsEnabled == enabled) {
                return ReturnCode::SUCCESS;
            }
            if (unsupportedReason != "") {
                return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, unsupportedReason);
            }
            
            if (enabled) {
                auto error = jvmti->SetExtensionEventCallback(mountEventIdx, (jvmtiExtensionEvent) &vtMountHandler);
                if (error != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set mount event handler, error code is  ", error);
                }
                error = jvmti->SetExtensionEventCallback(unmountEventIdx, (jvmtiExtensionEvent) &vtUnmountHandler);
                if (error != JVMTI_ERROR_NONE) {
                    jvmti->SetExtensionEventCallback(mountEventIdx, nullptr);
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set unmount event handler, error code is  ", error);
                }
                error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, static_cast<jvmtiEvent>(mountEventIdx), nullptr);
                if (error != JVMTI_ERROR_NONE) {
                    jvmti->SetExtensionEventCallback(unmountEventIdx, nullptr);
                    jvmti->SetExtensionEventCallback(mountEventIdx, nullptr);
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set mount event enabled, error code is  ", error);
                }
                error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, static_cast<jvmtiEvent>(unmountEventIdx), nullptr);
                if (error != JVMTI_ERROR_NONE) {
                    jvmti->SetEventNotificationMode(JVMTI_DISABLE, static_cast<jvmtiEvent>(mountEventIdx), nullptr);
                    jvmti->SetExtensionEventCallback(unmountEventIdx, nullptr);
                    jvmti->SetExtensionEventCallback(mountEventIdx, nullptr);
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set unmount event enabled, error code is  ", error);
                }
                eventsEnabled = true;
            } else {
                auto err1 = jvmti->SetEventNotificationMode(JVMTI_DISABLE, static_cast<jvmtiEvent>(mountEventIdx), nullptr);
                auto err2 = jvmti->SetEventNotificationMode(JVMTI_DISABLE, static_cast<jvmtiEvent>(unmountEventIdx), nullptr);
                auto err3 = jvmti->SetExtensionEventCallback(mountEventIdx, nullptr);
                auto err4 = jvmti->SetExtensionEventCallback(unmountEventIdx, nullptr);
                eventsEnabled = false;
                if (err1 != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set mount event mode to disabled, error code is  ", err1);
                }
                if (err2 != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to set unmount event mode to disabled, error code is  ", err2);
                }
                if (err3 != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to unset mount event handler, error code is  ", err3);
                }
                if (err4 != JVMTI_ERROR_NONE) {
                    return raiseExceptionAndReturn(jniEnv, ReturnCode::ERROR, "Failed to to unset unmount event handler, error code is  ", err4);
                }
            }
            return ReturnCode::SUCCESS;
        }
    }
}