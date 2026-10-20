package com.phongchen.toolbox.feature.speed

/**
 * Pure speed/distance math and trip statistics, JVM-testable.
 * Unit conversions and the moving-only average policy live here so screens stay dumb.
 */
object SpeedEngine {

    const val MS_TO_KMH = 3.6f
    const val MS_TO_MPH = 2.236936f
    const val METERS_PER_KM = 1000f
    const val METERS_PER_MILE = 1609.344f

    /** Below this speed a fix counts as "stationary" and is excluded from the average. */
    const val MOVING_THRESHOLD_MPS = 0.5f

    fun kmh(metersPerSecond: Float): Float = metersPerSecond * MS_TO_KMH
    fun mph(metersPerSecond: Float): Float = metersPerSecond * MS_TO_MPH
    fun km(meters: Float): Float = meters / METERS_PER_KM
    fun miles(meters: Float): Float = meters / METERS_PER_MILE
}

/**
 * Accumulates trip statistics from GPS fixes.
 * Average covers moving samples only (>= [SpeedEngine.MOVING_THRESHOLD_MPS]) so long stops
 * don't drag the figure down; max and trip distance accumulate regardless.
 */
class SpeedAccumulator {

    private var movingCount = 0
    private var movingSpeedSum = 0.0
    var maxMps: Float = 0f
        private set
    var tripMeters: Float = 0f
        private set

    val averageMps: Float
        get() = if (movingCount == 0) 0f else (movingSpeedSum / movingCount).toFloat()

    /** Record one fix; [distanceM] is the incremental distance since the previous fix. */
    fun add(speedMps: Float, distanceM: Float = 0f) {
        if (speedMps > maxMps) maxMps = speedMps
        if (speedMps >= SpeedEngine.MOVING_THRESHOLD_MPS) {
            movingCount += 1
            movingSpeedSum += speedMps
        }
        if (distanceM > 0f) tripMeters += distanceM
    }

    fun reset() {
        movingCount = 0
        movingSpeedSum = 0.0
        maxMps = 0f
        tripMeters = 0f
    }
}
