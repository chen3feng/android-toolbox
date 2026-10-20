package com.phongchen.toolbox.feature.altimeter

import com.phongchen.toolbox.core.prefs.ToolSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class AltimeterEngineTest {

    @Test
    fun standardPressureAtSeaLevel_isZero() {
        val altitude = AltimeterEngine.pressureAltitudeMeters(
            pressureHpa = ToolSettings.STANDARD_QNH_HPA.toFloat(),
            qnhHpa = ToolSettings.STANDARD_QNH_HPA,
        )
        assertEquals(0f, altitude, 0.5f)
    }

    @Test
    fun lowerPressureMeansHigherAltitude() {
        val seaLevel = AltimeterEngine.pressureAltitudeMeters(1013.25f, 1013.25)
        val onMountain = AltimeterEngine.pressureAltitudeMeters(850f, 1013.25)
        assertEquals(0f, seaLevel, 0.5f)
        // 850 hPa corresponds to roughly 1450 m.
        assertEquals(1450f, onMountain, 60f)
    }

    @Test
    fun qnhBackSolve_roundTripsKnownAltitude() {
        val qnh = 1013.25
        val trueAltitude = 1200.0
        // Pressure at that altitude under the chosen QNH.
        val pressure = (qnh * (1.0 - trueAltitude / 44330.0).pow(5.255)).toFloat()

        val solvedQnh = AltimeterEngine.qnhForKnownAltitude(pressure, trueAltitude)
        assertEquals(qnh, solvedQnh, 0.01)

        // And feeding the solved QNH back returns the known altitude.
        val back = AltimeterEngine.pressureAltitudeMeters(pressure, solvedQnh)
        assertEquals(trueAltitude.toFloat(), back, 0.5f)
    }

    @Test
    fun feetConversion() {
        assertEquals(3.28084f, AltimeterEngine.metersToFeet(1f), 1e-6f)
    }
}
