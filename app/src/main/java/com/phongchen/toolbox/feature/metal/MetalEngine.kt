package com.phongchen.toolbox.feature.metal

import kotlin.math.sqrt

object MetalEngine {

    const val MIN_UT = 0f
    const val MAX_UT = 1_000f

    /** Above this the field clearly deviates from the Earth's background (~25–65 µT). */
    const val STRONG_UT = 200f

    /** Total magnetic field magnitude in microtesla. */
    fun magnitudeUt(values: FloatArray): Float {
        var sum = 0.0
        for (v in values) sum += v.toDouble() * v
        return sqrt(sum).toFloat()
    }

    /** Exponential smoothing, same time-weighting idea as the other sensor tools. */
    fun lowPass(previous: Float, current: Float, newWeight: Float): Float =
        previous + (current - previous) * newWeight

    fun gaugeFraction(value: Float): Float =
        ((value - MIN_UT) / (MAX_UT - MIN_UT)).coerceIn(0f, 1f)
}
