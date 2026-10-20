package com.phongchen.toolbox.feature.sound

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Pure sound-level math, JVM-testable.
 *
 * Phone microphones report PCM amplitude, not acoustic pressure, so absolute SPL is only
 * approximated via a user-calibrated gain offset: SPL = dBFS + gain.
 */
object SoundMeterEngine {

    /** 16-bit PCM full-scale amplitude. */
    const val PCM_MAX = 32767.0

    /** Crude default mapping; the user should calibrate against a known source. */
    const val DEFAULT_GAIN_DB = 90.0

    const val MIN_DBFS = -90.0
    const val MAX_DBFS = 0.0

    /** Plausible ambient SPL window used for the gauge. */
    const val MIN_SPL_DB = 30.0
    const val MAX_SPL_DB = 110.0

    /** Root-mean-square of one PCM window; 0 for empty input. */
    fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sumSq = 0.0
        for (s in samples) {
            val v = s.toDouble()
            sumSq += v * v
        }
        return sqrt(sumSq / samples.size)
    }

    /** RMS to decibels relative to full scale, clamped to [-90, 0]. */
    fun dbFs(rms: Double): Double {
        if (rms <= 0.0) return MIN_DBFS
        return (20.0 * log10(rms / PCM_MAX)).coerceIn(MIN_DBFS, MAX_DBFS)
    }

    /** Estimated sound pressure level for a measured dBFS value and calibration gain. */
    fun splDb(dbFs: Double, calibrationGainDb: Double): Double = dbFs + calibrationGainDb

    /** Gain that maps the measured level onto a known reference SPL (calibration). */
    fun calibrationGain(measuredDbFs: Double, knownSplDb: Double): Double =
        knownSplDb - measuredDbFs

    /** Exponential smoothing (newWeight in 0..1), used to steady the readout. */
    fun smooth(previous: Double, current: Double, newWeight: Double): Double {
        val w = newWeight.coerceIn(0.0, 1.0)
        return previous + (current - previous) * w
    }

    /** 0..1 gauge position for an SPL value. */
    fun gaugeFraction(splDb: Double): Float =
        ((splDb - MIN_SPL_DB) / (MAX_SPL_DB - MIN_SPL_DB)).toFloat().coerceIn(0f, 1f)
}
