package com.phongchen.toolbox.feature.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs



class LevelEngineTest {

    @Test
    fun flatPhone_isZeroTilt() {
        // Android convention: a flat face-up device at rest reads gravity along +Z.
        val tilt = LevelEngine.tilt(floatArrayOf(0f, 0f, 9.81f))
        assertEquals(0f, tilt.pitchDeg, 1e-3f)
        assertEquals(0f, tilt.rollDeg, 1e-3f)
        assertTrue(LevelEngine.isLevel(tilt))
    }

    @Test
    fun tippingRightEdgeDown_producesRoll() {
        // Rotated 10 degrees about the Y axis, right edge down: g -> (+g sin, 0, +g cos).
        val angle = Math.toRadians(10.0)
        val gx = (9.81 * Math.sin(angle)).toFloat()
        val gz = (9.81 * Math.cos(angle)).toFloat()
        val tilt = LevelEngine.tilt(floatArrayOf(gx, 0f, gz))
        assertEquals(0f, tilt.pitchDeg, 0.5f)
        assertEquals(10f, tilt.rollDeg, 0.5f)
        assertFalse(LevelEngine.isLevel(tilt))
    }

    @Test
    fun tippingTopEdgeDown_producesPitch() {
        val angle = Math.toRadians(15.0)
        val gy = (9.81 * Math.sin(angle)).toFloat()
        val gz = (9.81 * Math.cos(angle)).toFloat()
        val tilt = LevelEngine.tilt(floatArrayOf(0f, gy, gz))
        assertEquals(15f, abs(tilt.pitchDeg), 0.5f)
        assertEquals(0f, tilt.rollDeg, 0.5f)
    }

    @Test
    fun applyZero_subtractsReference() {
        val raw = Tilt(pitchDeg = 5f, rollDeg = -3f)
        val calibrated = LevelEngine.applyZero(raw, zeroPitch = 5f, zeroRoll = 0f)
        assertEquals(0f, calibrated.pitchDeg, 1e-4f)
        assertEquals(-3f, calibrated.rollDeg, 1e-4f)
    }

    @Test
    fun toleranceClassifiesNearLevel() {
        assertTrue(LevelEngine.isLevel(Tilt(0.5f, -0.5f)))
        assertFalse(LevelEngine.isLevel(Tilt(0.7f, 0f)))
    }
}
