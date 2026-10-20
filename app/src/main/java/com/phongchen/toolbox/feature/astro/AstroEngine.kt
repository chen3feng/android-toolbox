package com.phongchen.toolbox.feature.astro

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure-JVM astronomy math for the Sun & Moon tool: solar time, equation of time,
 * sunrise/sunset and lunar phase.
 *
 * Solar formulas follow the NOAA Solar Calculator / the "Sunrise equation" (low
 * precision, ~0.1° / <1 min accuracy); lunar longitudes use the principal-term
 * abridgment of Meeus, *Astronomical Algorithms* ch. 47 (accuracy ~0.5°, more than
 * enough for a phase icon). Longitudes are east-positive, angles in degrees unless
 * stated otherwise.
 */
object AstroEngine {

    private const val J2000_JD = 2451545.0
    private const val DAY_MS = 86_400_000.0
    private const val SECONDS_PER_DAY = 86_400.0
    private const val SYNODIC_MONTH_DAYS = 29.530588853

    // Standard sunrise zenith: 90°50' (34' refraction + 16' solar semi-diameter).
    private const val SUNRISE_ZENITH_DEG = 90.8333

    /** Unix epoch millis (UT) -> fractional Julian day. */
    fun julianDay(epochMillis: Long): Double = epochMillis / DAY_MS + 2440587.5

    /** Fractional Julian day -> Unix epoch millis (UT). */
    fun epochMillisFromJulianDay(jd: Double): Long =
        ((jd - 2440587.5) * DAY_MS).roundToLong()

    /** Julian day at 00:00 UT for a calendar date. */
    fun julianDayAtMidnightUtc(date: LocalDate): Double =
        fliegelJulianDayAtNoon(date.year, date.monthValue, date.dayOfMonth) - 0.5

    private fun julianCentury(jd: Double): Double = (jd - J2000_JD) / 36525.0

    private fun norm360(deg: Double): Double {
        var r = deg % 360.0
        if (r < 0.0) r += 360.0
        return r
    }

    private fun wrapSeconds(seconds: Double): Double {
        var r = seconds % SECONDS_PER_DAY
        if (r < 0.0) r += SECONDS_PER_DAY
        return r
    }

    // ---------------------------------------------------------------------
    // Sun position (NOAA low precision)
    // ---------------------------------------------------------------------

    private fun geomMeanLongSun(t: Double): Double =
        norm360(280.46646 + t * (36000.76983 + 0.0003032 * t))

    private fun geomMeanAnomalySun(t: Double): Double =
        357.52911 + t * (35999.05029 - 0.0001537 * t)

    private fun eccentricityEarth(t: Double): Double =
        0.016708634 - t * (0.000042037 + 0.0000001267 * t)

    private fun sunEquationOfCenter(t: Double): Double {
        val m = Math.toRadians(geomMeanAnomalySun(t))
        return sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2.0 * m) * (0.019993 - 0.000101 * t) +
            sin(3.0 * m) * 0.000289
    }

    private fun sunApparentLong(t: Double): Double {
        val trueLong = geomMeanLongSun(t) + sunEquationOfCenter(t)
        return trueLong - 0.00569 - 0.00478 * sin(Math.toRadians(125.04 - 1934.136 * t))
    }

    private fun meanObliquityEcliptic(t: Double): Double {
        val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
        return 23.0 + (26.0 + seconds / 60.0) / 60.0
    }

    private fun obliquityCorrection(t: Double): Double =
        meanObliquityEcliptic(t) + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * t))

    /** Solar declination in degrees for a Julian day. */
    fun solarDeclinationDeg(jd: Double): Double {
        val t = julianCentury(jd)
        return Math.toDegrees(
            asin(
                sin(Math.toRadians(obliquityCorrection(t))) *
                    sin(Math.toRadians(sunApparentLong(t))),
            ),
        )
    }

    /**
     * Equation of time in minutes: positive when the sundial is ahead of mean time.
     */
    fun equationOfTimeMinutes(jd: Double): Double {
        val t = julianCentury(jd)
        val epsilon = obliquityCorrection(t)
        val l0 = geomMeanLongSun(t)
        val e = eccentricityEarth(t)
        val m = geomMeanAnomalySun(t)
        var y = tan(Math.toRadians(epsilon / 2.0))
        y *= y

        val sin2l0 = sin(2.0 * Math.toRadians(l0))
        val sinm = sin(Math.toRadians(m))
        val cos2l0 = cos(2.0 * Math.toRadians(l0))
        val sin4l0 = sin(4.0 * Math.toRadians(l0))
        val sin2m = sin(2.0 * Math.toRadians(m))

        val eqTimeRadians = y * sin2l0 -
            2.0 * e * sinm +
            4.0 * e * y * sinm * cos2l0 -
            0.5 * y * y * sin4l0 -
            1.25 * e * e * sin2m
        return 4.0 * Math.toDegrees(eqTimeRadians)
    }

    /**
     * Local solar times for an instant.
     *
     * @param epochMillis  current time (UT)
     * @param longitudeDeg observer longitude, east positive
     * @return seconds-of-day (0..86400) for mean and apparent (true) solar time.
     */
    fun solarTimes(epochMillis: Long, longitudeDeg: Double): SolarTimes {
        val jd = julianDay(epochMillis)
        val utcSeconds = (epochMillis / 1000.0).let {
            val dayStart = floor(it / SECONDS_PER_DAY) * SECONDS_PER_DAY
            it - dayStart
        }
        // 1 degree of longitude = 4 minutes of time.
        val mean = wrapSeconds(utcSeconds + longitudeDeg * 240.0)
        val apparent = wrapSeconds(mean + equationOfTimeMinutes(jd) * 60.0)
        return SolarTimes(
            meanSecondsOfDay = mean,
            apparentSecondsOfDay = apparent,
            equationOfTimeSeconds = equationOfTimeMinutes(jd) * 60.0,
        )
    }

    data class SolarTimes(
        val meanSecondsOfDay: Double,
        val apparentSecondsOfDay: Double,
        val equationOfTimeSeconds: Double,
    )

    // ---------------------------------------------------------------------
    // Sunrise / sunset (Sunrise equation)
    // ---------------------------------------------------------------------

    data class SunTimes(
        /** Julian days (UT); null when the sun never rises/sets that local date. */
        val sunriseJd: Double?,
        val sunsetJd: Double?,
        val transitJd: Double,
        /** True = polar night (stays below), false = polar day/midnight sun. */
        val polarNight: Boolean = false,
    ) {
        val sunNeverRises: Boolean get() = sunriseJd == null && sunsetJd == null && polarNight
        val sunNeverSets: Boolean get() = sunriseJd == null && sunsetJd == null && !polarNight
    }

    /**
     * Sunrise, transit and sunset for [date] interpreted in the observer's *local*
     * calendar (the date the phone displays). [latitudeDeg]/[longitudeDeg] east
     * positive. Returns null rise/set under polar day/night.
     */
    fun sunTimes(date: LocalDate, latitudeDeg: Double, longitudeDeg: Double): SunTimes {
        // Julian day at 00:00 UT of the same numbered date; the longitude term in n
        // shifts the anchor to the local observing day.
        val jdStart = julianDayAtMidnightUtc(date)

        // Longitude here enters with west-positive sign in the underlying formula,
        // hence + on the day count and - on the transit anchor for east longitude.
        val n = ceil(jdStart - J2000_JD + 0.0008 + longitudeDeg / 360.0)
        val jStar = J2000_JD + 0.0009 - longitudeDeg / 360.0 + n

        // Solar mean anomaly, equation of center and ecliptic longitude (degrees).
        val m = norm360(357.5291 + 0.98560028 * (jStar - J2000_JD))
        val center = 1.9148 * sin(Math.toRadians(m)) +
            0.0200 * sin(2.0 * Math.toRadians(m)) +
            0.0003 * sin(3.0 * Math.toRadians(m))
        val lambda = norm360(m + center + 180.0 + 102.9372)

        val jTransit = jStar +
            0.0053 * sin(Math.toRadians(m)) -
            0.0069 * sin(2.0 * Math.toRadians(lambda))

        val sinDecl = sin(Math.toRadians(lambda)) * sin(Math.toRadians(23.4397))
        val decl = asin(sinDecl)
        val lat = Math.toRadians(latitudeDeg)
        val cosHourAngle = (sin(Math.toRadians(-0.833)) - sin(lat) * sinDecl) /
            (cos(lat) * cos(decl))

        return if (cosHourAngle > 1.0 || cosHourAngle < -1.0) {
            // cosH > 1: never rises (polar night); cosH < -1: never sets (midnight sun).
            SunTimes(null, null, jTransit, polarNight = cosHourAngle > 1.0)
        } else {
            val omega = Math.toDegrees(acos(cosHourAngle)) / 360.0
            SunTimes(
                sunriseJd = jTransit - omega,
                sunsetJd = jTransit + omega,
                transitJd = jTransit,
            )
        }
    }

    /**
     * Altitude of the Sun at local solar noon, in degrees.
     * `90 - |latitude - declination|`; may exceed 90 only via rounding (clamped).
     */
    fun solarNoonAltitudeDeg(latitudeDeg: Double, transitJd: Double): Double {
        val decl = solarDeclinationDeg(transitJd)
        return (90.0 - abs(latitudeDeg - decl)).coerceIn(0.0, 90.0)
    }

    // ---------------------------------------------------------------------
    // Moon (Meeus ch. 47, abridged principal terms)
    // ---------------------------------------------------------------------

    data class MoonInfo(
        /** Illuminated fraction 0..1. */
        val illumination: Double,
        /** Apparent elongation of the Moon from the Sun in degrees, 0..360. */
        val elongationDeg: Double,
        /** Phase index 0..7: new, waxing crescent, first quarter, waxing gibbous,
         *  full, waning gibbous, last quarter, waning crescent. */
        val phaseIndex: Int,
        /** Mean age in days since the (apparent) new moon. */
        val ageDays: Double,
    )

    fun moonInfo(epochMillis: Long): MoonInfo {
        val jd = julianDay(epochMillis)
        val t = julianCentury(jd)

        // Sun apparent ecliptic longitude.
        val sunLon = sunApparentLong(t)

        // Moon mean elements (degrees).
        val lPrime = norm360(
            218.3164477 + 481267.88123421 * t - 0.0015924 * t * t +
                t * t * t / 538841.0 - t * t * t * t / 65194000.0,
        )
        val d = Math.toRadians(
            norm360(
                297.8501921 + 445267.1114034 * t - 0.0018819 * t * t +
                    t * t * t / 545868.0 - t * t * t * t / 113065000.0,
            ),
        )
        val mSun = Math.toRadians(
            357.5291092 + 35999.0502909 * t - 0.0001536 * t * t -
                t * t * t / 24490000.0,
        )
        val mMoon = Math.toRadians(
            134.9633964 + 477198.8675055 * t + 0.0087414 * t * t +
                t * t * t / 69699.0 - t * t * t * t / 14712000.0,
        )

        // Principal longitude perturbations (arguments in radians, terms degrees):
        // evection, variation, annual equation and the next-largest terms.
        var lon = lPrime +
            6.289 * sin(mMoon) -
            1.274 * sin(d - mMoon) +
            0.658 * sin(d) -
            0.186 * sin(mSun) -
            0.059 * sin(2.0 * d - mMoon) -
            0.057 * sin(d - mMoon - mSun) +
            0.053 * sin(d + mMoon) +
            0.046 * sin(d - mSun) +
            0.041 * sin(mMoon - mSun) -
            0.035 * sin(d + mMoon - mSun)
        lon = norm360(lon)

        // Elongation of the moon from the sun; the latitude term is <= 5°, ignored.
        val elongation = Math.toRadians(norm360(lon - sunLon))
        val illumination = (1.0 - cos(elongation)) / 2.0
        val elongationDeg = Math.toDegrees(elongation)
        val phaseIndex = floor((elongationDeg / 45.0) + 0.5).toInt() and 7
        val ageDays = elongationDeg / 360.0 * SYNODIC_MONTH_DAYS
        return MoonInfo(illumination, elongationDeg, phaseIndex, ageDays)
    }

    // ---------------------------------------------------------------------
    // Formatting helpers (kept here so they are unit-tested with the math)
    // ---------------------------------------------------------------------

    /** Seconds-of-day -> HH:MM:SS (or HH:MM when [withSeconds] is false). */
    fun formatClock(secondsOfDay: Double, withSeconds: Boolean = true): String {
        val total = floor(wrapSeconds(secondsOfDay)).toInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (withSeconds) {
            "%02d:%02d:%02d".format(h, m, s)
        } else {
            "%02d:%02d".format(h, m)
        }
    }

    /** Signed duration like +3m12s / -0m45s for the equation of time. */
    fun formatSignedDuration(seconds: Double): String {
        val total = floor(abs(seconds) + 0.5).toInt()
        val sign = if (seconds < 0.0) "−" else "+"
        return "%s%dm%02ds".format(sign, total / 60, total % 60)
    }

    /** Day length between two Julian days, formatted H小时mm分 style handled by caller;
     *  returns seconds. */
    fun daylightSeconds(sunriseJd: Double?, sunsetJd: Double?): Double? =
        if (sunriseJd != null && sunsetJd != null) {
            (sunsetJd - sunriseJd) * SECONDS_PER_DAY
        } else {
            null
        }

    // Fliegel-Van Flandern integer Julian day (valid 1 Mar 0 onwards).
    private fun fliegelJulianDayAtNoon(year: Int, month: Int, day: Int): Double {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return (day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045).toDouble()
    }
}
