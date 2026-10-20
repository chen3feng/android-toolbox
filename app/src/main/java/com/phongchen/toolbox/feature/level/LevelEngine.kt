package com.phongchen.toolbox.feature.level

import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.sqrt

/** Device tilt, degrees. Both zero when the phone lies flat face-up. */
data class Tilt(
    val pitchDeg: Float,
    val rollDeg: Float,
)

object LevelEngine {

    const val LEVEL_TOLERANCE_DEG: Float = 0.6f

    /**
     * Tilt from a gravity / accelerometer vector in the Android device frame
     * (x right, y toward the top edge, z out of the screen; a flat face-up device
     * at rest reports (0, 0, +g)).
     */
    fun tilt(gravity: FloatArray): Tilt {
        val magnitude = sqrt(
            gravity[0] * gravity[0] +
                gravity[1] * gravity[1] +
                gravity[2] * gravity[2],
        )
        if (magnitude == 0f) return Tilt(0f, 0f)
        val gx = gravity[0] / magnitude
        val gy = gravity[1] / magnitude
        val gz = gravity[2] / magnitude

        // Pitch: top edge tipping down is positive. Roll: right edge tipping down is positive.
        // Adding +0 normalises the "-0.0" that atan2 returns for a flat face-up device.
        val pitch = Math.toDegrees(atan2(gy, gz).toDouble()).toFloat() + 0f
        val roll = Math.toDegrees(atan2(gx, gz).toDouble()).toFloat() + 0f
        return Tilt(pitch, roll)
    }

    /** Subtract a user-set zero reference, wrapping back into (-180, 180]. */
    fun applyZero(tilt: Tilt, zeroPitch: Float, zeroRoll: Float): Tilt =
        Tilt(
            pitchDeg = wrap180(tilt.pitchDeg - zeroPitch) + 0f,
            rollDeg = wrap180(tilt.rollDeg - zeroRoll) + 0f,
        )

    fun isLevel(tilt: Tilt, toleranceDeg: Float = LEVEL_TOLERANCE_DEG): Boolean =
        abs(tilt.pitchDeg) <= toleranceDeg && abs(tilt.rollDeg) <= toleranceDeg

    private fun wrap180(deg: Float): Float = ((deg + 180f) % 360f + 360f) % 360f - 180f
}
