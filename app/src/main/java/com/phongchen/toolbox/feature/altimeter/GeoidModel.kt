package com.phongchen.toolbox.feature.altimeter

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Offline EGM96 geoid undulation model.
 *
 * GPS reports height above the WGS84 ellipsoid (HAE); the everyday "elevation above
 * sea level" is the orthometric height h = H − N, where N is the geoid undulation.
 * The bundled asset is a 0.5° × 0.5° grid of N (int16 centimeters, little-endian,
 * 361 rows from 90°N to 90°S × 720 columns 0..359.5°E), an exact node subset of the
 * official NGA 15-arcminute grid WW15MGH.DAC. Catmull-Rom bicubic interpolation
 * keeps the worst-case error vs the 15′ grid under about 2 m.
 */
class GeoidModel private constructor(private val data: ShortArray) {

    /**
     * Geoid undulation N in meters at [latitude]/[longitude] (east positive).
     * Orthometric height = ellipsoidal height − N.
     */
    fun undulationM(latitude: Double, longitude: Double): Double =
        GeoidInterp.undulation(data, ROWS, COLS, STEP_DEG, latitude, longitude)

    companion object {
        const val ASSET_NAME = "egm96_0_5.bin"
        const val ROWS = 361
        const val COLS = 720
        const val STEP_DEG = 0.5
        private const val BYTES = ROWS * COLS * 2

        /** Loads the bundled grid; null if the asset is missing/corrupt. */
        fun load(context: Context): GeoidModel? = runCatching {
            val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
            require(bytes.size == BYTES) { "Bad geoid asset size: ${bytes.size}" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val shorts = ShortArray(ROWS * COLS)
            buffer.asShortBuffer().get(shorts)
            GeoidModel(shorts)
        }.getOrNull()
    }
}

/** Pure interpolation over an int16-centimeter grid, no Android dependencies. */
object GeoidInterp {

    fun undulation(
        data: ShortArray,
        rows: Int,
        cols: Int,
        stepDeg: Double,
        latitude: Double,
        longitude: Double,
    ): Double {
        var lon = longitude % 360.0
        if (lon < 0.0) lon += 360.0
        val lat = latitude.coerceIn(-90.0 + stepDeg, 90.0 - stepDeg)

        val fy = (90.0 - lat) / stepDeg
        val fx = lon / stepDeg
        val r0 = fy.toInt().coerceAtMost(rows - 2)
        val c0Base = fx.toInt().coerceAtMost(cols - 1)
        val v = fy - r0
        val u = fx - c0Base

        val column = DoubleArray(4)
        for (i in 0..3) {
            val r = (r0 + i - 1).coerceIn(0, rows - 1)
            val p = DoubleArray(4)
            for (j in 0..3) {
                val c = ((c0Base + j - 1) % cols + cols) % cols // wraps at the dateline
                p[j] = data[r * cols + c].toDouble() / 100.0
            }
            column[i] = catmullRom(p[0], p[1], p[2], p[3], u)
        }
        return catmullRom(column[0], column[1], column[2], column[3], v)
    }

    /** Catmull-Rom cubic; reproduces constant and linear sequences exactly. */
    private fun catmullRom(p0: Double, p1: Double, p2: Double, p3: Double, x: Double): Double {
        val a = -0.5 * p0 + 1.5 * p1 - 1.5 * p2 + 0.5 * p3
        val b = p0 - 2.5 * p1 + 2.0 * p2 - 0.5 * p3
        val c = -0.5 * p0 + 0.5 * p2
        return ((a * x + b) * x + c) * x + p1
    }
}
