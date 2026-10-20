package com.phongchen.toolbox.feature.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates metronome clicks and continuous tones with a single streaming [AudioTrack].
 * Only one mode plays at a time; [stop] releases the track immediately.
 */
class AudioEngine {

    @Volatile
    var bpm: Int = 100

    @Volatile
    var beatsPerMeasure: Int = 4

    @Volatile
    var frequencyHz: Double = 440.0

    @Volatile
    var waveform: WaveformType = WaveformType.SINE

    @Volatile
    var volume: Float = 0.5f

    /** Called from the audio thread with the 0-based beat within the measure. */
    @Volatile
    var onBeat: ((Int) -> Unit)? = null

    private val lock = Any()
    private var mode = Mode.IDLE
    private var track: AudioTrack? = null
    private var worker: Thread? = null

    private enum class Mode { IDLE, METRONOME, TONE }

    fun startMetronome() = synchronized(lock) {
        if (mode == Mode.METRONOME) return
        stopLocked()
        mode = Mode.METRONOME
        val audioTrack = newTrack()
        track = audioTrack
        audioTrack.play()
        worker = Thread({ runMetronome(audioTrack) }, "toolbox-metronome").apply { start() }
    }

    fun startTone() = synchronized(lock) {
        if (mode == Mode.TONE) return
        stopLocked()
        mode = Mode.TONE
        val audioTrack = newTrack()
        track = audioTrack
        audioTrack.play()
        worker = Thread({ runTone(audioTrack) }, "toolbox-tone").apply { start() }
    }

    fun isPlaying(): Boolean = synchronized(lock) { mode != Mode.IDLE }

    fun stop() = synchronized(lock) { stopLocked() }

    private fun stopLocked() {
        mode = Mode.IDLE
        worker?.interrupt()
        worker = null
        track?.let { t ->
            runCatching { t.stop() }
            runCatching { t.flush() }
            t.release()
        }
        track = null
    }

    private fun newTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            AudioMath.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(AudioMath.SAMPLE_RATE) // ~1 s floor
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(AudioMath.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuffer * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.setVolume(volume) }
    }

    private fun runMetronome(audioTrack: AudioTrack) {
        val downbeat = AudioMath.clickSamples(1_500.0, durationMs = 70, gain = 0.9)
        val click = AudioMath.clickSamples(900.0, durationMs = 50, gain = 0.6)
        var beat = 0
        while (mode == Mode.METRONOME) {
            val start = System.nanoTime()
            val buffer = if (beat % beatsPerMeasure == 0) downbeat else click
            audioTrack.setVolume(volume)
            audioTrack.write(buffer, 0, buffer.size)
            onBeat?.invoke(beat % beatsPerMeasure)
            beat++
            val nextAt = start + AudioMath.metronomeIntervalNanos(bpm)
            while (mode == Mode.METRONOME) {
                val remaining = (nextAt - System.nanoTime()) / 1_000_000
                if (remaining <= 0) break
                try {
                    Thread.sleep(remaining.coerceAtMost(5))
                } catch (e: InterruptedException) {
                    return
                }
            }
        }
    }

    private fun runTone(audioTrack: AudioTrack) {
        val chunk = ShortArray(2_048)
        var phase = 0.0
        while (mode == Mode.TONE) {
            val increment = AudioMath.phaseIncrement(frequencyHz)
            val amplitude = (volume * Short.MAX_VALUE).toInt()
            for (i in chunk.indices) {
                chunk[i] = (AudioMath.sample(phase, waveform) * amplitude).toInt().toShort()
                phase += increment
                if (phase >= 2.0 * PI) phase -= 2.0 * PI
            }
            audioTrack.write(chunk, 0, chunk.size)
        }
    }
}
