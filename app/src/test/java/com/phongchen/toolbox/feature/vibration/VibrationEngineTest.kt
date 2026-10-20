package com.phongchen.toolbox.feature.vibration

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VibrationEngineTest {

    @Test
    fun magnitude_isVectorNorm() {
        assertEquals(10f, VibrationEngine.magnitude(floatArrayOf(0f, 0f, 10f)), 1e-4f)
        assertEquals(5f, VibrationEngine.magnitude(floatArrayOf(3f, 4f)), 1e-4f)
    }

    @Test
    fun lowPass_blendsEachAxis() {
        val result = VibrationEngine.lowPass(
            floatArrayOf(0f, 10f),
            floatArrayOf(10f, 20f),
            0.5f,
        )
        assertArrayEquals(floatArrayOf(5f, 15f), result, 1e-6f)
    }

    @Test
    fun gaugeFraction_mapsAndClamps() {
        assertEquals(0.5f, VibrationEngine.gaugeFraction(10f), 1e-6f)
        assertEquals(1f, VibrationEngine.gaugeFraction(40f), 1e-6f)
    }

    @Test
    fun level_thresholds() {
        assertEquals(0, VibrationEngine.level(0.4f))
        assertEquals(1, VibrationEngine.level(0.5f))
        assertEquals(1, VibrationEngine.level(1.9f))
        assertEquals(2, VibrationEngine.level(2f))
        assertEquals(2, VibrationEngine.level(4.9f))
        assertEquals(3, VibrationEngine.level(5f))
        assertEquals(3, VibrationEngine.level(50f))
    }
}
