package com.phongchen.toolbox.feature.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class AudioMathTest {

    @Test
    fun metronomeInterval_scalesWithBpm() {
        assertEquals(500_000_000L, AudioMath.metronomeIntervalNanos(120))
        assertEquals(1_000_000_000L, AudioMath.metronomeIntervalNanos(60))
    }

    @Test
    fun metronomeInterval_zeroBpmDoesNotCrash() {
        assertEquals(60_000_000_000L, AudioMath.metronomeIntervalNanos(0))
    }

    @Test
    fun phaseIncrement_oneHertzIsOneCyclePerSecond() {
        assertEquals(2.0 * PI / AudioMath.SAMPLE_RATE, AudioMath.phaseIncrement(1.0), 1e-12)
    }

    @Test
    fun waveforms_stayWithinRangeAndRespectPolarity() {
        assertEquals(1.0, AudioMath.sample(PI / 2, WaveformType.SINE), 1e-9)
        assertEquals(1.0, AudioMath.sample(PI / 2, WaveformType.SQUARE), 1e-9)
        assertEquals(-1.0, AudioMath.sample(3 * PI / 2, WaveformType.SQUARE), 1e-9)
        assertEquals(0.0, AudioMath.sample(0.0, WaveformType.SAWTOOTH), 1e-9)
        assertEquals(-1.0, AudioMath.sample(PI, WaveformType.SAWTOOTH), 1e-9)

        for (i in 0 until 100) {
            val phase = i * 0.13
            abs(AudioMath.sample(phase, WaveformType.SAWTOOTH)).also {
                assertTrue("sawtooth out of range at $phase", it <= 1.0 + 1e-9)
            }
        }
    }

    @Test
    fun clickSamples_hasExpectedLengthAndAmplitude() {
        val clicks = AudioMath.clickSamples(freqHz = 1_000.0, durationMs = 50, gain = 0.5)
        assertEquals(AudioMath.SAMPLE_RATE * 50 / 1000, clicks.size)
        assertEquals(0, clicks.first().toInt())
        assertTrue(clicks.all { it in -32_767..32_767 })
    }

    @Test
    fun frequencySlider_isLogarithmicAndReversible() {
        assertEquals(50.0, AudioMath.sliderToFrequency(0f), 1e-6)
        val max = AudioMath.sliderToFrequency(1f)
        assertTrue("max freq $max should be near 8000 Hz", max in 7_900.0..8_100.0)

        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { position ->
            val freq = AudioMath.sliderToFrequency(position)
            assertEquals(position, AudioMath.frequencyToSlider(freq), 1e-5f)
        }
    }
}
