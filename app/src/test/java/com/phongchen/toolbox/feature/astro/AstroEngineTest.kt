package com.phongchen.toolbox.feature.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs

class AstroEngineTest {

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
            .toInstant().toEpochMilli()

    @Test
    fun julianDay_j2000Epoch() {
        // 2000-01-01 12:00 UT is JD 2451545.0.
        val jd = AstroEngine.julianDay(utc(2000, 1, 1, 12, 0))
        assertEquals(2451545.0, jd, 1e-6)
    }

    @Test
    fun julianDayAtMidnightUtc_isHalfIntegerBeforeNoonJd() {
        val jd = AstroEngine.julianDayAtMidnightUtc(LocalDate.of(2000, 1, 1))
        assertEquals(2451544.5, jd, 1e-6)
    }

    @Test
    fun epochMillisRoundTrip() {
        val ms = utc(2026, 10, 20, 4, 0)
        assertEquals(
            ms.toDouble(),
            AstroEngine.epochMillisFromJulianDay(AstroEngine.julianDay(ms)).toDouble(),
            1.0,
        )
    }

    @Test
    fun solarDeclination_solstices() {
        val winter = AstroEngine.solarDeclinationDeg(AstroEngine.julianDay(utc(2000, 12, 21, 12, 0)))
        val summer = AstroEngine.solarDeclinationDeg(AstroEngine.julianDay(utc(2000, 6, 21, 12, 0)))
        assertEquals(-23.43, winter, 0.2)
        assertEquals(23.44, summer, 0.2)
    }

    @Test
    fun equationOfTime_yearlyRange() {
        // EoT always stays within roughly +/-16 minutes.
        var day = utc(2026, 1, 1, 12, 0)
        val stepMs = 86_400_000L
        var min = 100.0
        var max = -100.0
        repeat(365) {
            val eot = AstroEngine.equationOfTimeMinutes(AstroEngine.julianDay(day))
            min = minOf(min, eot)
            max = maxOf(max, eot)
            day += stepMs
        }
        // Observed yearly envelope is about -14.3 .. +16.5 minutes.
        assertTrue("min=$min", min > -15.0)
        assertTrue("max=$max", max < 17.0)
    }

    @Test
    fun meanSolarTime_isUtcPlusLongitude() {
        // 04:00 UT at 114.0859°E (Shenzhen): +7h36m20.6s.
        val times = AstroEngine.solarTimes(utc(2026, 10, 20, 4, 0), 114.0859)
        val expectedMean = 4 * 3600.0 + (114.0859 * 240.0)
        assertEquals(expectedMean % 86400.0, times.meanSecondsOfDay, 0.5)
    }

    @Test
    fun apparentSolarTime_appliesEquationOfTime() {
        val times = AstroEngine.solarTimes(utc(2026, 10, 20, 4, 0), 114.0859)
        // EoT around Oct 20 is about +15.5 minutes (sundial ahead).
        val eotMinutes = times.equationOfTimeSeconds / 60.0
        assertTrue("eot=$eotMinutes", eotMinutes in 13.0..17.0)
        val diff = times.apparentSecondsOfDay - times.meanSecondsOfDay
        assertEquals(eotMinutes * 60.0, diff, 0.5)
    }

    @Test
    fun sunTimes_shenzhenMidOctober() {
        // Shenzhen: published 2026-10-20 sunrise ≈ 06:28 CST, sunset ≈ 17:58 CST.
        val times = AstroEngine.sunTimes(LocalDate.of(2026, 10, 20), 22.5431, 114.0579)
        val cst = ZoneOffset.ofHours(8)
        fun toLocalSeconds(jd: Double?): Double {
            val zdt = java.time.Instant.ofEpochMilli(
                AstroEngine.epochMillisFromJulianDay(jd!!),
            ).atZone(cst)
            return zdt.hour * 3600.0 + zdt.minute * 60.0 + zdt.second.toDouble()
        }
        val riseLocal = toLocalSeconds(times.sunriseJd)
        val setLocal = toLocalSeconds(times.sunsetJd)
        println("Shenzhen rise=${AstroEngine.formatClock(riseLocal, false)} set=${AstroEngine.formatClock(setLocal, false)}")
        // 06:20..06:35, 17:50..18:05 local.
        assertTrue("rise=$riseLocal", riseLocal in (6 * 3600 + 20 * 60).toDouble()..(6 * 3600 + 35 * 60).toDouble())
        assertTrue("set=$setLocal", setLocal in (17 * 3600 + 50 * 60).toDouble()..(18 * 3600 + 5 * 60).toDouble())

        val daylight = AstroEngine.daylightSeconds(times.sunriseJd, times.sunsetJd)!!
        assertTrue("daylight=$daylight", daylight in 11.4 * 3600..11.7 * 3600)
    }

    @Test
    fun sunPosition_noonAndRiseSet() {
        // Beijing near summer-solstice transit (12:15 CST = 04:15 UT): due south, 73.5 deg.
        val noon = AstroEngine.sunPosition(utc(2026, 6, 21, 4, 15), 39.9, 116.4)
        assertTrue("az=${noon.azimuthDeg}", noon.azimuthDeg in 175.0..185.0)
        assertTrue("alt=${noon.altitudeDeg}", noon.altitudeDeg in 72.5..74.5)
        assertEquals(23.44, noon.declinationDeg, 0.1)

        // Shenzhen, mid-October, a few minutes after transit: sun past south.
        val sz = AstroEngine.sunPosition(utc(2026, 10, 20, 4, 24), 22.5431, 114.0579)
        assertTrue("az=${sz.azimuthDeg}", sz.azimuthDeg in 183.0..191.0)
        assertTrue("alt=${sz.altitudeDeg}", sz.altitudeDeg in 56.0..58.0)

        // At the computed sunrise instant, geometric altitude sits just under zero
        // (the -0.833 deg sunrise zenith includes refraction + radius), az ~ ESE.
        val times = AstroEngine.sunTimes(LocalDate.of(2026, 10, 20), 22.5431, 114.0579)
        val rise = AstroEngine.sunPosition(
            AstroEngine.epochMillisFromJulianDay(times.sunriseJd!!), 22.5431, 114.0579,
        )
        assertTrue("rise alt=${rise.altitudeDeg}", rise.altitudeDeg in -1.6..0.2)
        assertTrue("rise az=${rise.azimuthDeg}", rise.azimuthDeg in 95.0..106.0)
        val set = AstroEngine.sunPosition(
            AstroEngine.epochMillisFromJulianDay(times.sunsetJd!!), 22.5431, 114.0579,
        )
        // Rise/set come from the abridged sunrise equation (-0.833 deg zenith); tolerance
        // covers its ~0.4 deg cross-model offset against the full NOAA sun position.
        assertTrue("set alt=${set.altitudeDeg}", set.altitudeDeg in -1.6..0.2)
        assertTrue("set az=${set.azimuthDeg}", set.azimuthDeg in 254.0..265.0)

        // Deep night: sun well below the horizon.
        val night = AstroEngine.sunPosition(utc(2026, 10, 20, 13, 30), 22.5431, 114.0579)
        assertTrue("night alt=${night.altitudeDeg}", night.altitudeDeg < -40.0)
    }

    @Test
    fun sunPosition_longitudeMovesNoonByFourMinutesPerDegree() {
        // 15 degrees farther east transits the meridian one UT hour earlier.
        val a = AstroEngine.sunPosition(utc(2026, 6, 21, 4, 15), 39.9, 116.4)
        val b = AstroEngine.sunPosition(utc(2026, 6, 21, 3, 15), 39.9, 131.4)
        // Same sky configuration (azimuth within ~1 deg, altitude within ~0.3 deg).
        assertEquals(a.azimuthDeg, b.azimuthDeg, 1.0)
        assertEquals(a.altitudeDeg, b.altitudeDeg, 0.3)
    }

    @Test
    fun moonPosition_shenzhenNight() {
        // 2026-10-20 21:40 CST (13:40 UT): waxing gibbous high in the SSW sky.
        val m = AstroEngine.moonPosition(utc(2026, 10, 20, 13, 40), 22.54, 114.06)
        assertTrue("az=${m.azimuthDeg}", m.azimuthDeg in 208.0..224.0)
        assertTrue("alt=${m.altitudeDeg}", m.altitudeDeg in 38.0..52.0)
    }

    @Test
    fun moonPosition_fullMoonOpposesSun() {
        // At the 2024-09-18 full moon, somewhere on Earth the Moon is overhead and the
        // Sun is near the nadir: scan the globe for the sub-lunar point via altitudes.
        val ms = utc(2024, 9, 18, 2, 44)
        var maxMoonAlt = -99.0
        var sunAtSubMoon = 0.0
        for (lat in -80..80 step 10) {
            for (lon in 0..350 step 10) {
                val moon = AstroEngine.moonPosition(ms, lat.toDouble(), lon.toDouble())
                if (moon.altitudeDeg > maxMoonAlt) {
                    maxMoonAlt = moon.altitudeDeg
                    sunAtSubMoon = AstroEngine.sunPosition(ms, lat.toDouble(), lon.toDouble()).altitudeDeg
                }
            }
        }
        assertTrue("maxMoonAlt=$maxMoonAlt", maxMoonAlt > 80.0)
        assertTrue("sunAltAtSubMoon=$sunAtSubMoon", sunAtSubMoon < -70.0)
    }

    @Test
    fun solarNoonAltitude() {
        // Beijing: summer solstice noon ~73.6°, winter solstice ~26.7°.
        val summer = AstroEngine.sunTimes(LocalDate.of(2026, 6, 21), 39.9, 116.4)
        val winter = AstroEngine.sunTimes(LocalDate.of(2026, 12, 22), 39.9, 116.4)
        assertEquals(73.6, AstroEngine.solarNoonAltitudeDeg(39.9, summer.transitJd), 0.3)
        assertEquals(26.7, AstroEngine.solarNoonAltitudeDeg(39.9, winter.transitJd), 0.3)
        // Tropical site near overhead at the equinoxes.
        val equinox = AstroEngine.sunTimes(LocalDate.of(2026, 3, 20), 0.0, 0.0)
        assertEquals(89.9, AstroEngine.solarNoonAltitudeDeg(0.0, equinox.transitJd), 0.3)
    }

    @Test
    fun sunTimes_polarRegions() {
        // Tromsø in late December: polar night. July: midnight sun.
        val winter = AstroEngine.sunTimes(LocalDate.of(2026, 12, 22), 69.65, 18.96)
        assertTrue(winter.sunNeverRises)
        val summer = AstroEngine.sunTimes(LocalDate.of(2026, 7, 1), 69.65, 18.96)
        assertTrue(summer.sunNeverSets)
    }

    @Test
    fun moonPhase_knownAnchors() {
        // New moon 2000-01-06 18:14 UT (canonical almanac).
        val newMoon2000 = AstroEngine.moonInfo(utc(2000, 1, 6, 18, 14))
        assertTrue("illum=${newMoon2000.illumination}", newMoon2000.illumination < 0.005)
        assertEquals(0, newMoon2000.phaseIndex)

        // 2024 anchors: new Apr 8 18:21 (total solar eclipse), Q1 Apr 15 19:13,
        // full Sep 18 02:44, last quarter May 1 (~11-12 UT).
        val newMoon = AstroEngine.moonInfo(utc(2024, 4, 8, 18, 21))
        assertTrue("illum=${newMoon.illumination}", newMoon.illumination < 0.005)
        assertEquals(0, newMoon.phaseIndex)

        val firstQ = AstroEngine.moonInfo(utc(2024, 4, 15, 19, 13))
        assertTrue("fq illum=${firstQ.illumination}", abs(firstQ.illumination - 0.5) < 0.07)
        assertEquals(2, firstQ.phaseIndex)

        val lastQ = AstroEngine.moonInfo(utc(2024, 5, 1, 12, 0))
        assertTrue("lq illum=${lastQ.illumination}", abs(lastQ.illumination - 0.5) < 0.07)
        assertEquals(6, lastQ.phaseIndex)

        val fullMoon = AstroEngine.moonInfo(utc(2024, 9, 18, 2, 44))
        assertTrue("illum=${fullMoon.illumination}", fullMoon.illumination > 0.995)
        assertEquals(4, fullMoon.phaseIndex)
    }

    @Test
    fun moonAge_inSynodicRange() {
        val info = AstroEngine.moonInfo(utc(2026, 10, 20, 4, 0))
        assertTrue("age=${info.ageDays}", info.ageDays in 0.0..29.53)
        assertTrue(info.illumination in 0.0..1.0)
    }

    @Test
    fun formatClock_wrapsAndPads() {
        assertEquals("00:00:05", AstroEngine.formatClock(5.0))
        assertEquals("23:59:59", AstroEngine.formatClock(86399.4))
        assertEquals("01:02", AstroEngine.formatClock(3725.0, withSeconds = false))
        // Negative offsets wrap around the day.
        assertEquals("23:00:00", AstroEngine.formatClock(-3600.0))
    }

    @Test
    fun formatSignedDuration() {
        assertEquals("+3m12s", AstroEngine.formatSignedDuration(192.4))
        assertEquals("−0m45s", AstroEngine.formatSignedDuration(-44.6))
    }
}
