package com.phongchen.toolbox.feature.sound

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable

/** Mic is missing or already held by another client. */
class MicUnavailableException(message: String) : Exception(message)

/**
 * Thin [AudioRecord] wrapper delivering fixed-size PCM windows on a background dispatcher.
 * The caller is responsible for RECORD_AUDIO permission and for calling [stop] when the
 * screen leaves the foreground, so the mic is never held in the background.
 */
class MicRecorder(private val context: Context) {

    private var recorder: AudioRecord? = null
    private var job: Job? = null

    val isRecording: Boolean get() = job?.isActive == true

    /**
     * Starts capture; [onWindow] receives ~[WINDOW_MS] of 16-bit mono PCM at a time.
     * [onError] is invoked once if initialisation fails (busy mic, no mic).
     */
    @SuppressLint("MissingPermission") // permission is checked by the ViewModel before starting
    fun start(
        scope: CoroutineScope,
        onWindow: (ShortArray) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (isRecording) return
        job = scope.launch(Dispatchers.IO) {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) {
                onError(MicUnavailableException("getMinBufferSize failed: $minBuffer"))
                return@launch
            }
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2,
                )
            } catch (e: SecurityException) {
                onError(e)
                return@launch
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                onError(MicUnavailableException("AudioRecord not initialized (mic busy?)"))
                return@launch
            }
            recorder = record

            try {
                record.startRecording()
                val window = ShortArray(WINDOW_SAMPLES)
                var filled = 0
                val readBuffer = ShortArray(minBuffer / 2)
                while (isActive) {
                    val read = record.read(readBuffer, 0, readBuffer.size)
                    if (read <= 0) continue
                    var src = 0
                    while (src < read && isActive) {
                        val take = minOf(read - src, WINDOW_SAMPLES - filled)
                        System.arraycopy(readBuffer, src, window, filled, take)
                        src += take
                        filled += take
                        if (filled == WINDOW_SAMPLES) {
                            onWindow(window.copyOf())
                            filled = 0
                        }
                    }
                }
            } finally {
                // The job is already on Dispatchers.IO; use NonCancellable so a cancelled
                // collector still actually stops and releases the AudioRecord (otherwise the
                // mic stays open in the background).
                withContext(NonCancellable) {
                    runCatching { record.stop() }
                    record.release()
                    recorder = null
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val WINDOW_MS = 250
        const val WINDOW_SAMPLES = SAMPLE_RATE * WINDOW_MS / 1000
    }
}
