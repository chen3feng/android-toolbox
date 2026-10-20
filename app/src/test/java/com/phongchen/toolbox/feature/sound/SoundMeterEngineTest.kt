package com.phongchen.toolbox.feature.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SoundMeterEngineTest {

    @Test
    fun rms_emptyArray_isZero() {
        assertEquals(0.0, SoundMeterEngine.rms(ShortArray(0)), 1e-9)
    }

    @Test
    fun rms_constantSignal_equalsAmplitude() {
        val samples = ShortArray(64) { 1000 }
        assertEquals(1000.0, SoundMeterEngine.rms(samples), 1e-6)
    }

    @Test
    fun rms_fullScaleSine_isAmplitudeOverRoot2() {
        val n = 256
        val samples = ShortArray(n) { i ->
            (SoundMeterEngine.PCM_MAX * Math.sin(2.0 * Math.PI * i / n)).toInt().toShort()
        }
        val expected = SoundMeterEngine.PCM_MAX / sqrt(2.0)
        assertEquals(expected, SoundMeterEngine.rms(samples), 200.0) // quantisation tolerance
    }

    @Test
    fun dbFs_silence_clampedToFloor() {
        assertEquals(-90.0, SoundMeterEngine.dbFs(0.0), 1e-9)
    }

    @Test
    fun dbFs_fullScale_isZero() {
        assertEquals(0.0, SoundMeterEngine.dbFs(SoundMeterEngine.PCM_MAX), 1e-6)
    }

    @Test
    fun dbFs_halfScale_isAboutMinus6dB() {
        assertEquals(-6.02, SoundMeterEngine.dbFs(SoundMeterEngine.PCM_MAX / 2.0), 0.01)
    }

    @Test
    fun splDb_appliesCalibrationGain() {
        assertEquals(84.0, SoundMeterEngine.splDb(dbFs = -6.0, calibrationGainDb = 90.0), 1e-6)
    }

    @Test
    fun calibrationGain_mapsMeasurementToReference() {
        // A 60 dB reference measured as -30 dBFS needs a +90 dB gain.
        val gain = SoundMeterEngine.calibrationGain(measuredDbFs = -30.0, knownSplDb = 60.0)
        assertEquals(90.0, gain, 1e-6)
        assertEquals(60.0, SoundMeterEngine.splDb(-30.0, gain), 1e-6)
    }

    @Test
    fun smooth_weightsNewSample() {
        assertEquals(5.0, SoundMeterEngine.smooth(previous = 0.0, current = 10.0, newWeight = 0.5), 1e-9)
        assertEquals(10.0, SoundMeterEngine.smooth(0.0, 10.0, 1.0), 1e-9)
        assertEquals(0.0, SoundMeterEngine.smooth(0.0, 10.0, 0.0), 1e-9)
    }

    @Test
    fun smooth_clampsOutOfRangeWeights() {
        assertEquals(10.0, SoundMeterEngine.smooth(0.0, 10.0, 2.0), 1e-9)
        assertEquals(0.0, SoundMeterEngine.smooth(0.0, 10.0, -1.0), 1e-9)
    }

    @Test
    fun gaugeFraction_mapsRangeAndClamps() {
        assertEquals(0f, SoundMeterEngine.gaugeFraction(30.0), 1e-6f)
        assertEquals(1f, SoundMeterEngine.gaugeFraction(110.0), 1e-6f)
        assertEquals(0.5f, SoundMeterEngine.gaugeFraction(70.0), 1e-6f)
        assertEquals(0f, SoundMeterEngine.gaugeFraction(0.0), 1e-6f)
        assertEquals(1f, SoundMeterEngine.gaugeFraction(200.0), 1e-6f)
        assertTrue(SoundMeterEngine.gaugeFraction(70.0) in 0f..1f)
    }
}
