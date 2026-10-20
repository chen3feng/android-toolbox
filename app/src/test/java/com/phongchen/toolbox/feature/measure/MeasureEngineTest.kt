package com.phongchen.toolbox.feature.measure

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureEngineTest {

    @Test
    fun pixelsPerUnit_followDpi() {
        assertEquals(160f, MeasureEngine.pxPerInch(160f), 1e-6f)
        assertEquals(160f / 2.54f, MeasureEngine.pxPerCm(160f), 1e-4f)
    }

    @Test
    fun relativeAngle_passthroughWithZeroBaseline() {
        assertEquals(10f, MeasureEngine.relativeAngle(10f, 0f), 1e-4f)
    }

    @Test
    fun relativeAngle_wrapsAcrossCircle() {
        assertEquals(-10f, MeasureEngine.relativeAngle(350f, 0f), 1e-4f)
        // Current 20°, zero mark at 350° → 30° apart.
        assertEquals(30f, MeasureEngine.relativeAngle(20f, 350f), 1e-4f)
        // Exactly opposite stays positive 180.
        assertEquals(180f, MeasureEngine.relativeAngle(180f, 0f), 1e-4f)
    }

    @Test
    fun relativeAngle_subtractsZeroOffset() {
        assertEquals(10f, MeasureEngine.relativeAngle(20f, 10f), 1e-4f)
    }
}
