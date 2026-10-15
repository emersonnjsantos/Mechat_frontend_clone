package com.whatsapp.clone.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class RealAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0

    val isRecording: Boolean
        get() = recorder != null

    fun startRecording(): File? {
        val audioDir = File(context.cacheDir, "audio_notes").apply { mkdirs() }
        val file = File(audioDir, "audio_msg_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        recorder = try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    MediaRecorder(context)
                } catch (t: Throwable) {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("RealAudioRecorder", "Failed to start MediaRecorder: ${e.message}", e)
            try { recorder?.release() } catch (ignored: Exception) {}
            recorder = null
            currentOutputFile = null
            return null
        }

        startTimeMs = System.currentTimeMillis()
        Log.i("RealAudioRecorder", "MediaRecorder successfully started: ${file.absolutePath}")
        return file
    }

    fun getMaxAmplitude(): Float {
        return try {
            val maxAmp = recorder?.maxAmplitude ?: 0
            // Normalize between 0.1f and 1.0f
            (maxAmp.toFloat() / 20000f).coerceIn(0.1f, 1.0f)
        } catch (e: Exception) {
            0.2f
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val durationMs = System.currentTimeMillis() - startTimeMs
        if (durationMs < 600) {
            try {
                Thread.sleep(600 - durationMs)
            } catch (e: InterruptedException) {
                // Thread interrupted
            }
        }

        val finalDurationMs = System.currentTimeMillis() - startTimeMs
        val durationSec = (finalDurationMs / 1000).toInt().coerceAtLeast(1)

        try {
            recorder?.stop()
            Log.i("RealAudioRecorder", "MediaRecorder stopped successfully. Duration: ${durationSec}s")
        } catch (e: Exception) {
            Log.e("RealAudioRecorder", "Error stopping recorder: ${e.message}", e)
        } finally {
            try {
                recorder?.release()
            } catch (e: Exception) {
                // Release error
            }
            recorder = null
        }

        val file = currentOutputFile
        currentOutputFile = null
        return Pair(file, durationSec)
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // Ignore cancel error
        } finally {
            recorder?.release()
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }

    // Playback Subsystem
    fun playAudio(
        file: File?,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        stopAudio()

        if (file == null || !file.exists()) {
            onComplete()
            return
        }

        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopAudio()
                    onComplete()
                }
            }
        } catch (e: IOException) {
            Log.e("RealAudioRecorder", "Error playing audio file: ${e.message}")
            stopAudio()
            onComplete()
        }
    }

    fun stopAudio() {
        try {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            player?.release()
        } catch (e: Exception) {
            // Safe release
        } finally {
            player = null
        }
    }

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    fun getAudioProgress(): Float {
        val p = player ?: return 0f
        return try {
            if (p.duration > 0) {
                p.currentPosition.toFloat() / p.duration.toFloat()
            } else 0f
        } catch (e: Exception) {
            0f
        }
    }
}
