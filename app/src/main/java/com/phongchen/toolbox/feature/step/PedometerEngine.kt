package com.phongchen.toolbox.feature.step

object PedometerEngine {

    /** Rough average adult step length; estimates only. */
    const val STEP_LENGTH_M = 0.74f

    /** Common approximation for casual walking. */
    const val KCAL_PER_STEP = 0.04f

    const val M_TO_KM = 0.001f
    const val M_TO_MILE = 0.000621371f

    /** Counter sensor reports a boot-relative total; the session is total minus our baseline. */
    fun sessionSteps(total: Float, baseline: Float?): Float =
        if (baseline == null) 0f else (total - baseline).coerceAtLeast(0f)

    fun distanceMeters(steps: Float): Float = steps * STEP_LENGTH_M

    fun caloriesKcal(steps: Float): Float = steps * KCAL_PER_STEP
}
