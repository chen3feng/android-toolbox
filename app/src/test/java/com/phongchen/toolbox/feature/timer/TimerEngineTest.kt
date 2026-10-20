package com.phongchen.toolbox.feature.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {

    @Test
    fun elapsed_zero() {
        assertEquals("00:00.00", TimerEngine.formatElapsed(0))
    }

    @Test
    fun elapsed_belowAnHour() {
        // 1 min 5.43 s
        assertEquals("01:05.43", TimerEngine.formatElapsed(65_430))
    }

    @Test
    fun elapsed_atAndAboveAnHour() {
        assertEquals("1:01:01.23", TimerEngine.formatElapsed(3_661_230))
        assertEquals("10:00:00.00", TimerEngine.formatElapsed(36_000_000))
    }

    @Test
    fun countdown_formatsWithoutCentiseconds() {
        assertEquals("00:00", TimerEngine.formatCountdown(0))
        assertEquals("09:59", TimerEngine.formatCountdown(599_000))
        assertEquals("1:00:00", TimerEngine.formatCountdown(3_600_000))
    }

    @Test
    fun countdown_negativeClampsToZero() {
        assertEquals("00:00", TimerEngine.formatCountdown(-5_000))
    }
}
