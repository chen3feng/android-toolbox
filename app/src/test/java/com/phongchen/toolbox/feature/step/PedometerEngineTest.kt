package com.phongchen.toolbox.feature.step

import org.junit.Assert.assertEquals
import org.junit.Test

class PedometerEngineTest {

    @Test
    fun sessionSteps_subtractsBaseline() {
        assertEquals(0f, PedometerEngine.sessionSteps(1_000f, null), 1e-6f)
        assertEquals(120f, PedometerEngine.sessionSteps(1_120f, 1_000f), 1e-6f)
    }

    @Test
    fun sessionSteps_neverGoesNegative() {
        // Sensor total can dip after a reboot while our baseline is stale.
        assertEquals(0f, PedometerEngine.sessionSteps(100f, 500f), 1e-6f)
    }

    @Test
    fun distanceAndCalories() {
        assertEquals(74f, PedometerEngine.distanceMeters(100f), 1e-4f)
        assertEquals(4f, PedometerEngine.caloriesKcal(100f), 1e-6f)
        assertEquals(0.074f, PedometerEngine.distanceMeters(100f) * PedometerEngine.M_TO_KM, 1e-6f)
    }
}
