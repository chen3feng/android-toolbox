package com.phongchen.toolbox.feature.compass

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure heading math, JVM-testable. Rotation-matrix code is a straight port of the
 * documented AOSP SensorManager formulas (same constants and sign conventions).
 */
object CompassEngine {

    /** Heading in degrees 0..360 from a rotation-vector sensor event. */
    fun azimuthFromRotationVector(rotationVector: FloatArray): Float {
        val rotation = FloatArray(9)
        rotationMatrixFromVector(rotation, rotationVector)
        return azimuthFromRotationMatrix(rotation)
    }

    /**
     * Heading from the legacy accelerometer + magnetometer pair, or null when the field
     * is too disturbed / gravity undefined to build a rotation matrix.
     */
    fun azimuthFromAccelMag(accelerometer: FloatArray, magneticField: FloatArray): Float? {
        val rotation = FloatArray(9)
        if (!rotationMatrix(rotation, accelerometer, magneticField)) return null
        return azimuthFromRotationMatrix(rotation)
    }

    /** Apply magnetic declination (east positive) to a magnetic heading. */
    fun withDeclination(magneticAzimuth: Float, declinationDeg: Float): Float =
        normalize(magneticAzimuth + declinationDeg)

    /** Wrap any angle into the half-open range [0, 360). */
    fun normalize(deg: Float): Float {
        var result = deg % 360f
        if (result < 0f) result += 360f
        // Guard against tiny negative-zero residues landing on 360.
        if (result >= 359.999f) result = 0f
        return result
    }

    /**
     * Angle-aware low-pass filter. Naive lerping visibly spins backwards when crossing
     * 0/360, so interpolate on the unit circle instead.
     *
     * @param newSampleWeight weight of [current] in 0..1; higher = snappier, lower = smoother.
     */
    fun lowPass(previous: Float, current: Float, newSampleWeight: Float): Float {
        val w = newSampleWeight.coerceIn(0f, 1f)
        val a = Math.toRadians(previous.toDouble())
        val b = Math.toRadians(current.toDouble())
        val x = cos(a) * (1.0 - w) + cos(b) * w
        val y = sin(a) * (1.0 - w) + sin(b) * w
        return normalize(Math.toDegrees(atan2(y, x)).toFloat())
    }

    private val DIRECTIONS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    /** 8-wind compass label. */
    fun directionLabel(azimuth: Float): String =
        DIRECTIONS[((normalize(azimuth) + 22.5f) / 45f).toInt() and 7]

    // --- rotation math (ported from android.hardware.SensorManager) ---

    private fun azimuthFromRotationMatrix(r: FloatArray): Float =
        // 3x3 row-major layout: azimuth = atan2(R[1], R[4]) (SensorManager.getOrientation).
        normalize(Math.toDegrees(atan2(r[1].toDouble(), r[4].toDouble())).toFloat())

    private fun rotationMatrixFromVector(r: FloatArray, rv: FloatArray) {
        var q0 = if (rv.size >= 4) rv[3] else 0f
        if (q0 >= 1f) {
            identityMatrix(r)
            return
        }
        val q1 = rv[0]
        val q2 = rv[1]
        val q3 = rv[2]
        if (rv.size < 4) {
            val d = 1f - q1 * q1 - q2 * q2 - q3 * q3
            q0 = sqrt(if (d > 0f) d else 0f)
        }
        val sq1 = 2f * q1 * q1
        val sq2 = 2f * q2 * q2
        val sq3 = 2f * q3 * q3
        val q1q2 = 2f * q1 * q2
        val q3q0 = 2f * q3 * q0
        val q1q3 = 2f * q1 * q3
        val q2q0 = 2f * q2 * q0
        val q2q3 = 2f * q2 * q3
        val q1q0 = 2f * q1 * q0

        r[0] = 1f - sq2 - sq3
        r[1] = q1q2 - q3q0
        r[2] = q1q3 + q2q0
        r[3] = q1q2 + q3q0
        r[4] = 1f - sq1 - sq3
        r[5] = q2q3 - q1q0
        r[6] = q1q3 - q2q0
        r[7] = q2q3 + q1q0
        r[8] = 1f - sq1 - sq2
    }

    private fun rotationMatrix(r: FloatArray, gravity: FloatArray, geomagnetic: FloatArray): Boolean {
        var ax = gravity[0]
        var ay = gravity[1]
        var az = gravity[2]
        // Reject near free-fall: gravity under 10% of standard (same guard as AOSP).
        if (ax * ax + ay * ay + az * az < 0.01f * 9.81f * 9.81f) return false
        val ex = geomagnetic[0]
        val ey = geomagnetic[1]
        val ez = geomagnetic[2]

        // H = E x A (east / north-plane component of the field)
        var hx = ey * az - ez * ay
        var hy = ez * ax - ex * az
        var hz = ex * ay - ey * ax
        val normH = sqrt(hx * hx + hy * hy + hz * hz)
        // Near magnetic pole / free fall, same threshold as AOSP.
        if (normH < 0.1f) return false
        val invH = 1f / normH
        hx *= invH
        hy *= invH
        hz *= invH

        val invA = 1f / sqrt(ax * ax + ay * ay + az * az)
        ax *= invA
        ay *= invA
        az *= invA

        // M = A x H
        val mx = ay * hz - az * hy
        val my = az * hx - ax * hz
        val mz = ax * hy - ay * hx

        r[0] = hx
        r[1] = hy
        r[2] = hz
        r[3] = mx
        r[4] = my
        r[5] = mz
        r[6] = ax
        r[7] = ay
        r[8] = az
        return true
    }

    private fun identityMatrix(r: FloatArray) {
        r.fill(0f)
        r[0] = 1f
        r[4] = 1f
        r[8] = 1f
    }
}
