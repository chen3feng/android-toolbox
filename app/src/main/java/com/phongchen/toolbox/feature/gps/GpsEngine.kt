package com.phongchen.toolbox.feature.gps

import kotlin.math.abs

object GpsEngine {

    fun decimalDegrees(value: Double): String = "%.6f°".format(value)

    /** Signed decimal degree → DMS triple with hemisphere letter. */
    fun dms(value: Double, latitude: Boolean): String {
        val hemisphere = if (latitude) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
        val absolute = abs(value)
        val degrees = absolute.toInt()
        val minutesFull = (absolute - degrees) * 60.0
        val minutes = minutesFull.toInt()
        val seconds = (minutesFull - minutes) * 60.0
        return "%d°%02d′%04.1f″%s".format(degrees, minutes, seconds, hemisphere)
    }

    fun geoUri(lat: Double, lon: Double): String = "geo:%f,%f?q=%f,%f".format(lat, lon, lat, lon)

    fun shareText(lat: Double, lon: Double, altMeters: Double?): String = buildString {
        append("%.6f, %.6f".format(lat, lon))
        if (altMeters != null) append("\nAltitude: %.1f m".format(altMeters))
        append("\n").append(geoUri(lat, lon))
    }

    fun bearingLabel(bearing: Float): String? {
        if (bearing.isNaN()) return null
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing % 360f) + 22.5f).toInt() / 45 % 8
        return directions[index]
    }
}
