package com.phongchen.toolbox.feature.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassEngineTest {

    @Test
    fun normalize_wrapsIntoZeroTo360() {
        assertEquals(270f, CompassEngine.normalize(-90f), 1e-4f)
        assertEquals(10f, CompassEngine.normalize(370f), 1e-4f)
        assertEquals(0f, CompassEngine.normalize(0f), 1e-4f)
        assertEquals(359.5f, CompassEngine.normalize(-0.5f), 1e-4f)
    }

    @Test
    fun withDeclination_addsAngleAndWraps() {
        assertEquals(10f, CompassEngine.withDeclination(5f, 5f), 1e-4f)
        assertEquals(5f, CompassEngine.withDeclination(355f, 10f), 1e-4f)
    }

    @Test
    fun lowPass_doesNotSpinBackwardsAcrossZero() {
        // Smoothing between 350 and 10 must stay near 0, never interpolate through 180.
        val mid = CompassEngine.lowPass(previous = 350f, current = 10f, newSampleWeight = 0.5f)
        assertEquals(0f, mid, 1f)

        val mostlyNew = CompassEngine.lowPass(previous = 350f, current = 10f, newSampleWeight = 0.9f)
        assertTrue("got $mostlyNew", mostlyNew > 4f && mostlyNew < 16f)
    }

    @Test
    fun directionLabel_eightWinds() {
        assertEquals("N", CompassEngine.directionLabel(0f))
        assertEquals("N", CompassEngine.directionLabel(350f))
        assertEquals("NE", CompassEngine.directionLabel(45f))
        assertEquals("E", CompassEngine.directionLabel(100f))
        assertEquals("SW", CompassEngine.directionLabel(225f))
    }

    @Test
    fun azimuthFromRotationVector_identityPointsNorth() {
        // Zero rotation vector = identity orientation; azimuth must be ~0.
        val rv = FloatArray(5)
        rv[3] = 1f
        val azimuth = CompassEngine.azimuthFromRotationVector(rv)
        assertEquals(0f, azimuth, 0.5f)
    }

    @Test
    fun azimuthFromAccelMag_flatWithNorthFieldPointsNorth() {
        // Android convention: flat device screen-up reads accelerometer +Z.
        val acc = floatArrayOf(0f, 0f, 9.81f)
        val mag = floatArrayOf(0f, 30f, -40f)
        val azimuth = CompassEngine.azimuthFromAccelMag(acc, mag)
        assertEquals(0f, azimuth!!, 1f)
    }

    @Test
    fun azimuthFromAccelMag_yawRotationsTrackHeading() {
        // Facing east: field comes from device -X, still has downward component.
        val acc = floatArrayOf(0f, 0f, 9.81f)
        assertEquals(90f, CompassEngine.azimuthFromAccelMag(acc, floatArrayOf(-30f, 0f, -40f))!!, 1f)
        assertEquals(180f, CompassEngine.azimuthFromAccelMag(acc, floatArrayOf(0f, -30f, -40f))!!, 1f)
        assertEquals(270f, CompassEngine.azimuthFromAccelMag(acc, floatArrayOf(30f, 0f, -40f))!!, 1f)
    }

    @Test
    fun azimuthFromRotationVector_yawRotationsTrackHeading() {
        val q = kotlin.math.sqrt(0.5f)
        // Flat device, yaw about Z: negative q3 for clockwise (eastward) heading.
        assertEquals(90f, CompassEngine.azimuthFromRotationVector(floatArrayOf(0f, 0f, -q, q)), 0.5f)
        assertEquals(180f, CompassEngine.azimuthFromRotationVector(floatArrayOf(0f, 0f, -1f, 0f)), 0.5f)
        assertEquals(270f, CompassEngine.azimuthFromRotationVector(floatArrayOf(0f, 0f, q, q)), 0.5f)
    }

    @Test
    fun azimuthFromAccelMag_rejectsFreeFallAndNullField() {
        assertNull(CompassEngine.azimuthFromAccelMag(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 30f, -40f)))
        assertNull(CompassEngine.azimuthFromAccelMag(floatArrayOf(0f, 0f, 9.81f), floatArrayOf(0f, 0f, 0f)))
    }
}
