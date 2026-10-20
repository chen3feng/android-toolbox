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

    fun startMetronome(): Unit = synchronized(lock) {
        if (mode == Mode.METRONOME) return
        stopLocked()
        mode = Mode.METRONOME
        val audioTrack = newTrack()
        track = audioTrack
        audioTrack.play()
        worker = Thread({ runMetronome(audioTrack) }, "toolbox-metronome").apply { start() }
    }

    fun startTone(): Unit = synchronized(lock) {
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
        )
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
            // Render a CONTINUOUS beat cell: click followed by silence padding, exactly
            // one beat interval long. Blocking writes pace us against the audio clock,
            // so timing is sample-accurate even before the output finishes priming and
            // queued clicks can never burst out in a clump.
            val cellSamples = AudioMath.SAMPLE_RATE.toLong() * 60L / bpm.coerceAtLeast(1)
            val cell = ShortArray(cellSamples.toInt())
            val accent = if (beat % beatsPerMeasure == 0) downbeat else click
            accent.copyInto(cell, endIndex = minOf(accent.size, cell.size))
            audioTrack.setVolume(volume)
            if (!writeAll(audioTrack, cell)) return
            onBeat?.invoke(beat % beatsPerMeasure)
            beat++
        }
    }

    /** Blocking write of the whole array; false if playback was stopped/interrupted. */
    private fun writeAll(audioTrack: AudioTrack, data: ShortArray): Boolean {
        var offset = 0
        while (mode != Mode.IDLE && offset < data.size) {
            val written = audioTrack.write(data, offset, data.size - offset)
            if (written < 0) return false
            offset += written
        }
        return mode != Mode.IDLE
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
            if (!writeAll(audioTrack, chunk)) return
        }
    }
}
