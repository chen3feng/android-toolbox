package com.phongchen.toolbox.feature.vibration

import kotlin.math.sqrt

object VibrationEngine {

    const val MAX_MS2 = 20f
    const val WAVEFORM_CAPACITY = 200

    /** Acceleration magnitude (m/s²) of the vector. */
    fun magnitude(values: FloatArray): Float {
        var sum = 0.0
        for (v in values) sum += v.toDouble() * v
        return sqrt(sum).toFloat()
    }

    /** Low pass used to estimate gravity when only the raw accelerometer is available. */
    fun lowPass(previous: FloatArray, current: FloatArray, alpha: Float): FloatArray =
        FloatArray(current.size) { i -> previous[i] + (current[i] - previous[i]) * alpha }

    fun gaugeFraction(value: Float): Float = (value / MAX_MS2).coerceIn(0f, 1f)

    /** 0 = still, 1 = slight, 2 = noticeable, 3 = strong — for the status label. */
    fun level(value: Float): Int = when {
        value < 0.5f -> 0
        value < 2f -> 1
        value < 5f -> 2
        else -> 3
    }
}
