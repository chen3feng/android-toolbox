package com.phongchen.toolbox.feature.speed

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedEngineTest {

    @Test
    fun conversions() {
        assertEquals(36f, SpeedEngine.kmh(10f), 1e-4f) // 10 m/s = 36 km/h
        assertEquals(22.3694f, SpeedEngine.mph(10f), 0.001f)
        assertEquals(1.5f, SpeedEngine.km(1500f), 1e-4f)
        assertEquals(1f, SpeedEngine.miles(SpeedEngine.METERS_PER_MILE), 1e-4f)
    }

    @Test
    fun accumulator_empty_isZero() {
        val acc = SpeedAccumulator()
        assertEquals(0f, acc.averageMps, 1e-6f)
        assertEquals(0f, acc.maxMps, 1e-6f)
        assertEquals(0f, acc.tripMeters, 1e-6f)
    }

    @Test
    fun accumulator_tracksMaxAndAverageOfMovingSamples() {
        val acc = SpeedAccumulator()
        acc.add(2f)
        acc.add(4f)
        acc.add(6f)
        assertEquals(6f, acc.maxMps, 1e-6f)
        assertEquals(4f, acc.averageMps, 1e-6f)
    }

    @Test
    fun accumulator_stationarySamplesExcludedFromAverage() {
        val acc = SpeedAccumulator()
        acc.add(10f)
        acc.add(0f) // stop
        acc.add(0.2f) // below moving threshold
        assertEquals(10f, acc.averageMps, 1e-6f)
        assertEquals(10f, acc.maxMps, 1e-6f)
    }

    @Test
    fun accumulator_accumulatesTripDistance() {
        val acc = SpeedAccumulator()
        acc.add(5f, distanceM = 100f)
        acc.add(5f, distanceM = 50f)
        acc.add(0f, distanceM = 0f)
        assertEquals(150f, acc.tripMeters, 1e-6f)
    }

    @Test
    fun accumulator_negativeDistanceIgnored() {
        val acc = SpeedAccumulator()
        acc.add(5f, distanceM = 100f)
        acc.add(5f, distanceM = -10f)
        assertEquals(100f, acc.tripMeters, 1e-6f)
    }

    @Test
    fun accumulator_reset_clearsEverything() {
        val acc = SpeedAccumulator()
        acc.add(8f, distanceM = 500f)
        acc.reset()
        assertEquals(0f, acc.averageMps, 1e-6f)
        assertEquals(0f, acc.maxMps, 1e-6f)
        assertEquals(0f, acc.tripMeters, 1e-6f)
    }
}
