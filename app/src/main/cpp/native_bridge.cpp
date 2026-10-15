#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "crypto/curve25519.h"
#include "codec/opus_helper.h"

#define LOG_TAG "WhatsAppNativeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_whatsapp_clone_nativebridge_NativeBridge_getNativeCoreVersion(JNIEnv *env, jobject /* this */) {
    std::string version = "WhatsApp-Clone-NativeCore/2.26-Merged (C++17, Curve25519, Opus-MLOW, NEON)";
    return env->NewStringUTF(version.c_str());
}

JNIEXPORT jbyteArray JNICALL
Java_com_whatsapp_clone_nativebridge_NativeBridge_generateCurve25519PublicKey(
    JNIEnv *env,
    jobject /* this */,
    jbyteArray privateKeyArray
) {
    if (!privateKeyArray || env->GetArrayLength(privateKeyArray) != 32) {
        LOGE("Invalid private key length");
        return nullptr;
    }

    jbyte *privKey = env->GetByteArrayElements(privateKeyArray, nullptr);
    uint8_t pubKey[32] = {0};

    int res = curve25519_generate_public_key(pubKey, reinterpret_cast<const uint8_t *>(privKey));
    env->ReleaseByteArrayElements(privateKeyArray, privKey, JNI_ABORT);

    if (res != 0) {
        LOGE("Failed to generate Curve25519 public key");
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<const jbyte *>(pubKey));
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_whatsapp_clone_nativebridge_NativeBridge_calculateCurve25519SharedSecret(
    JNIEnv *env,
    jobject /* this */,
    jbyteArray privateKeyArray,
    jbyteArray peerPublicKeyArray
) {
    if (!privateKeyArray || env->GetArrayLength(privateKeyArray) != 32 ||
        !peerPublicKeyArray || env->GetArrayLength(peerPublicKeyArray) != 32) {
        LOGE("Invalid key parameters for shared secret");
        return nullptr;
    }

    jbyte *privKey = env->GetByteArrayElements(privateKeyArray, nullptr);
    jbyte *peerKey = env->GetByteArrayElements(peerPublicKeyArray, nullptr);
    uint8_t sharedSecret[32] = {0};

    int res = curve25519_eval(
        sharedSecret,
        reinterpret_cast<const uint8_t *>(privKey),
        reinterpret_cast<const uint8_t *>(peerKey)
    );

    env->ReleaseByteArrayElements(privateKeyArray, privKey, JNI_ABORT);
    env->ReleaseByteArrayElements(peerPublicKeyArray, peerKey, JNI_ABORT);

    if (res != 0) {
        LOGE("Failed to compute Curve25519 shared secret");
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<const jbyte *>(sharedSecret));
    return result;
}

JNIEXPORT jfloat JNICALL
Java_com_whatsapp_clone_nativebridge_NativeBridge_calculateAudioAmplitude(
    JNIEnv *env,
    jobject /* this */,
    jshortArray pcmSamplesArray
) {
    if (!pcmSamplesArray) return 0.0f;
    jsize length = env->GetArrayLength(pcmSamplesArray);
    if (length == 0) return 0.0f;

    jshort *samples = env->GetShortArrayElements(pcmSamplesArray, nullptr);
    float amp = opus_calculate_waveform_amplitude(
        reinterpret_cast<const int16_t *>(samples),
        static_cast<size_t>(length)
    );
    env->ReleaseShortArrayElements(pcmSamplesArray, samples, JNI_ABORT);
    return amp;
}

JNIEXPORT jboolean JNICALL
Java_com_whatsapp_clone_nativebridge_NativeBridge_isHardwareAccelerationAvailable(
    JNIEnv *env,
    jobject /* this */
) {
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

} // extern "C"
