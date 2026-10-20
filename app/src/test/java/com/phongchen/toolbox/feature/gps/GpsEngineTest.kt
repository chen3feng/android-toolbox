package com.phongchen.toolbox.feature.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsEngineTest {

    @Test
    fun decimalDegrees_sixDecimalsWithSuffix() {
        assertEquals("12.345679°", GpsEngine.decimalDegrees(12.3456789))
        assertEquals("-0.500000°", GpsEngine.decimalDegrees(-0.5))
    }

    @Test
    fun dms_convertsAndPicksHemisphere() {
        assertEquals("40°30′00.0″N", GpsEngine.dms(40.5, latitude = true))
        assertEquals("40°30′00.0″S", GpsEngine.dms(-40.5, latitude = true))
        assertEquals("75°15′00.0″E", GpsEngine.dms(75.25, latitude = false))
        assertEquals("75°15′00.0″W", GpsEngine.dms(-75.25, latitude = false))
    }

    @Test
    fun geoUri_containsCoordinates() {
        val uri = GpsEngine.geoUri(30.0, 120.0)
        assertTrue(uri, uri.startsWith("geo:30.000000,120.000000?q=30.000000,120.000000"))
    }

    @Test
    fun shareText_includesAltitudeOnlyWhenPresent() {
        val without = GpsEngine.shareText(30.0, 120.0, null)
        assertTrue(without.startsWith("30.000000, 120.000000"))
        assertTrue(without.contains("geo:"))

        val withAlt = GpsEngine.shareText(30.0, 120.0, 55.0)
        assertTrue(withAlt.contains("Altitude: 55.0 m"))
    }

    @Test
    fun bearingLabel_eightWinds() {
        assertEquals("N", GpsEngine.bearingLabel(0f))
        assertEquals("NE", GpsEngine.bearingLabel(45f))
        assertEquals("E", GpsEngine.bearingLabel(90f))
        assertEquals("SE", GpsEngine.bearingLabel(135f))
        assertEquals("S", GpsEngine.bearingLabel(180f))
        assertEquals("SW", GpsEngine.bearingLabel(225f))
        assertEquals("W", GpsEngine.bearingLabel(270f))
        assertEquals("NW", GpsEngine.bearingLabel(315f))
        // 350° sits within ±22.5° of north.
        assertEquals("N", GpsEngine.bearingLabel(350f))
    }

    @Test
    fun bearingLabel_nanIsNull() {
        assertNull(GpsEngine.bearingLabel(Float.NaN))
    }
}
