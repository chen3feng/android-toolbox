package com.phongchen.toolbox.feature.altimeter

import kotlin.math.pow

/** Barometric altitude math, pure Kotlin (same ISA formula SensorManager.getAltitude uses). */
object AltimeterEngine {

    const val METERS_TO_FEET: Float = 3.28084f
    private const val EXPONENT = 1.0 / 5.255

    /** Pressure altitude in meters for the given QNH (both pressures in hPa). */
    fun pressureAltitudeMeters(pressureHpa: Float, qnhHpa: Double): Float =
        (44330.0 * (1.0 - (pressureHpa.toDouble() / qnhHpa).pow(EXPONENT))).toFloat()

    /**
     * Back-solve the sea-level reference pressure from a known altitude and the current
     * pressure, used by the "set known altitude" calibration. Inverse of
     * h = 44330 * (1 - (p/p0)^(1/5.255)).
     */
    fun qnhForKnownAltitude(pressureHpa: Float, knownAltitudeMeters: Double): Double {
        val factor = 1.0 - knownAltitudeMeters / 44330.0
        require(factor > 0.0) { "Altitude must be below the tropopause for this calibration" }
        return pressureHpa.toDouble() / factor.pow(5.255)
    }

    fun metersToFeet(meters: Float): Float = meters * METERS_TO_FEET
}
