#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <android/log.h>

#define LOG_TAG "X11CoreJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global reference to the native window
static ANativeWindow* g_nativeWindow = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeInitServer(JNIEnv *env, jobject thiz,
                                                                  jstring socket_path) {
    const char *socketPathCStr = env->GetStringUTFChars(socket_path, nullptr);
    LOGI("nativeInitServer called with socketPath: %s", socketPathCStr);
    
    // Server initialization logic would go here
    
    env->ReleaseStringUTFChars(socket_path, socketPathCStr);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeSurfaceCreated(JNIEnv *env, jobject thiz,
                                                                      jobject surface) {
    LOGI("nativeSurfaceCreated called");
    if (g_nativeWindow != nullptr) {
        ANativeWindow_release(g_nativeWindow);
        g_nativeWindow = nullptr;
    }
    
    g_nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (g_nativeWindow == nullptr) {
        LOGE("Failed to obtain ANativeWindow from surface");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeSurfaceChanged(JNIEnv *env, jobject thiz,
                                                                      jobject surface, jint width,
                                                                      jint height) {
    LOGI("nativeSurfaceChanged called with width: %d, height: %d", width, height);
    if (g_nativeWindow != nullptr) {
        int32_t result = ANativeWindow_setBuffersGeometry(g_nativeWindow, width, height, WINDOW_FORMAT_RGBA_8888);
        if (result < 0) {
            LOGE("Failed to set buffers geometry");
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeSurfaceDestroyed(JNIEnv *env, jobject thiz) {
    LOGI("nativeSurfaceDestroyed called");
    if (g_nativeWindow != nullptr) {
        ANativeWindow_release(g_nativeWindow);
        g_nativeWindow = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeSendTouchEvent(JNIEnv *env, jobject thiz,
                                                                      jint action, jint pointer_id,
                                                                      jfloat x, jfloat y,
                                                                      jfloat pressure,
                                                                      jint tool_type) {
    // Touch event handling logic would go here
    // LOGI("nativeSendTouchEvent: action=%d, id=%d, x=%.2f, y=%.2f, p=%.2f, tool=%d", action, pointer_id, x, y, pressure, tool_type);
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_ilamparithi_aournalpp_x11_X11CoreBridge_nativeSendKeyEvent(JNIEnv *env, jobject thiz,
                                                                    jint key_code,
                                                                    jint unicode_char,
                                                                    jboolean is_down) {
    // Key event handling logic would go here
    // LOGI("nativeSendKeyEvent: keyCode=%d, char=%d, down=%d", key_code, unicode_char, is_down);
}
