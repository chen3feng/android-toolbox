package com.phongchen.toolbox.feature.altimeter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeoidInterpTest {

    private fun loadAssetGrid(): ShortArray {
        val file = File("src/main/assets/${GeoidModel.ASSET_NAME}")
        assumeTrue("geoid asset not found at $file", file.exists())
        val bytes = file.readBytes()
        assertEquals(GeoidModel.ROWS * GeoidModel.COLS * 2, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = ShortArray(GeoidModel.ROWS * GeoidModel.COLS)
        buffer.asShortBuffer().get(shorts)
        return shorts
    }

    private fun undulation(grid: ShortArray, lat: Double, lon: Double) =
        GeoidInterp.undulation(
            grid,
            GeoidModel.ROWS,
            GeoidModel.COLS,
            GeoidModel.STEP_DEG,
            lat,
            lon,
        )

    // Independent EGM96 full-harmonic reference values (GeoidHeightsDotNet test suite).
    @Test
    fun globalAnchors_matchReferenceModel() {
        val grid = loadAssetGrid()
        val cases = arrayOf(
            doubleArrayOf(38.628155, 269.779155, -31.629),
            doubleArrayOf(-14.621217, 305.021114, -2.966),
            doubleArrayOf(46.874319, 102.448729, -43.572),
            doubleArrayOf(-23.617446, 133.874712, 15.868),
            doubleArrayOf(38.625473, 359.9995, 50.065),
            doubleArrayOf(-0.466744, 0.0023, 17.330),
        )
        for ((lat, lon, expected) in cases) {
            val n = undulation(grid, lat, lon)
            assertTrue("N($lat,$lon)=$n vs $expected", kotlin.math.abs(n - expected) < 0.6)
        }
    }

    @Test
    fun longitudeWrapsAcrossDateline() {
        val grid = loadAssetGrid()
        val a = undulation(grid, 35.0, -0.25)
        val b = undulation(grid, 35.0, 359.75)
        assertEquals(a, b, 1e-6)
    }

    @Test
    fun constantGridIsExact() {
        val data = ShortArray(20 * 10) { 1234 } // 12.34 m everywhere
        assertEquals(
            12.34,
            GeoidInterp.undulation(data, 10, 20, 10.0, 12.3, -45.6),
            1e-9,
        )
    }

    @Test
    fun linearRampIsReproducedExactly() {
        // N grows 1 cm per longitude column and 2 cm per row southward.
        val rows = 10
        val cols = 12
        val data = ShortArray(rows * cols) { idx ->
            val r = idx / cols
            val c = idx % cols
            (c * 1 - r * 2).toShort()
        }
        // Interior fractional point.
        val n = GeoidInterp.undulation(data, rows, cols, 10.0, 10.0, 55.0)
        // r0=8,v=0 -> (-16 + c); c0=5 u=.5 -> mean of -11/-10 = -10.5 cm
        assertEquals(-0.105, n, 1e-9)
    }

    @Test
    fun poleLatitudesAreClamped() {
        val grid = loadAssetGrid()
        // No crash at the poles; value equals the nearest interior row.
        undulation(grid, 90.0, 0.0)
        undulation(grid, -90.0, 0.0)
    }
}
