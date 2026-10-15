package com.whatsapp.clone.nativebridge

import android.util.Log

/**
 * JNI Bridge class communicating directly with libwhatsapp_native.so (C++17 and C99).
 * Provides hardware-accelerated Curve25519 cryptography, Opus VoIP frame handling,
 * and NEON SIMD acceleration checks.
 */
object NativeBridge {

    private const val TAG = "NativeBridge"

    init {
        try {
            System.loadLibrary("whatsapp_native")
            Log.i(TAG, "Successfully loaded native library: libwhatsapp_native.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libwhatsapp_native.so: ${e.message}")
        }
    }

    /**
     * Returns compilation version and enabled modules from the C++ core.
     */
    external fun getNativeCoreVersion(): String

    /**
     * Generates a 32-byte Curve25519 public key from a 32-byte private key.
     */
    external fun generateCurve25519PublicKey(privateKey: ByteArray): ByteArray?

    /**
     * Calculates a 32-byte shared secret using Curve25519 Montgomery ladder (RFC 7748).
     */
    external fun calculateCurve25519SharedSecret(
        privateKey: ByteArray,
        peerPublicKey: ByteArray
    ): ByteArray?

    /**
     * Calculates real-time audio amplitude for waveform visualization from PCM samples.
     */
    external fun calculateAudioAmplitude(pcmSamples: ShortArray): Float

    /**
     * Checks if ARM NEON hardware acceleration is available on the current CPU.
     */
    external fun isHardwareAccelerationAvailable(): Boolean
}
