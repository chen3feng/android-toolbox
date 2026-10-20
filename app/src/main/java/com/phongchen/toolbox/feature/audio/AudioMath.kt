package com.phongchen.toolbox.feature.audio

import kotlin.math.PI
import kotlin.math.sin

enum class WaveformType { SINE, SQUARE, SAWTOOTH }

/** Pure audio calculations, kept separate from Android's AudioTrack for unit testing. */
object AudioMath {

    const val SAMPLE_RATE = 44_100

    fun metronomeIntervalNanos(bpm: Int): Long = 60_000_000_000L / bpm.coerceAtLeast(1)

    fun phaseIncrement(freqHz: Double): Double = 2.0 * PI * freqHz / SAMPLE_RATE

    /** One sample for [phase] in radians in [0, 2π). */
    fun sample(phase: Double, type: WaveformType): Double = when (type) {
        WaveformType.SINE -> sin(phase)
        WaveformType.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
        WaveformType.SAWTOOTH -> {
            val normalized = phase / (2.0 * PI)
            2.0 * (normalized - kotlin.math.floor(normalized + 0.5))
        }
    }

    /**
     * Short percussive click: sine burst with exponential decay.
     * [gain] 0..1 scales loudness (downbeat clicks are louder).
     */
    fun clickSamples(freqHz: Double, durationMs: Int, gain: Double): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / count
            val envelope = Math.exp(-t * 18.0)
            (sin(2.0 * PI * freqHz * i / SAMPLE_RATE) * envelope * gain * Short.MAX_VALUE).toInt()
                .toShort()
        }
    }

    /** Map a 0..1 slider position logarithmically to 50..8000 Hz. */
    fun sliderToFrequency(position: Float): Double =
        50.0 * Math.pow(10.0, position.toDouble() * 2.204) // 10^2.204 ≈ 160 → ~8000 Hz

    fun frequencyToSlider(freq: Double): Float =
        (Math.log10(freq / 50.0) / 2.204).toFloat().coerceIn(0f, 1f)
}
