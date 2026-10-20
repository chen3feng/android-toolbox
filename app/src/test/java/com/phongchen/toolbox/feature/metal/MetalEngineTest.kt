package com.phongchen.toolbox.feature.metal

import org.junit.Assert.assertEquals
import org.junit.Test

class MetalEngineTest {

    @Test
    fun magnitude_isVectorNorm() {
        assertEquals(5f, MetalEngine.magnitudeUt(floatArrayOf(3f, 4f)), 1e-4f)
        assertEquals(
            13f,
            MetalEngine.magnitudeUt(floatArrayOf(3f, 4f, 12f)),
            1e-4f,
        )
    }

    @Test
    fun lowPass_blendsTowardCurrent() {
        assertEquals(15f, MetalEngine.lowPass(10f, 20f, 0.5f), 1e-6f)
        assertEquals(10f, MetalEngine.lowPass(10f, 99f, 0f), 1e-6f)
        assertEquals(99f, MetalEngine.lowPass(10f, 99f, 1f), 1e-6f)
    }

    @Test
    fun gaugeFraction_mapsAndClamps() {
        assertEquals(0f, MetalEngine.gaugeFraction(-50f), 1e-6f)
        assertEquals(0.5f, MetalEngine.gaugeFraction(500f), 1e-6f)
        assertEquals(1f, MetalEngine.gaugeFraction(1_500f), 1e-6f)
    }
}
