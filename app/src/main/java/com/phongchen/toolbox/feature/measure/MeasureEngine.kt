package com.phongchen.toolbox.feature.measure

object MeasureEngine {

    const val RULER_LENGTH_CM = 30
    const val RULER_LENGTH_INCHES = 12

    /** Physical pixels per centimetre, derived from the reported horizontal DPI. */
    fun pxPerCm(xdpi: Float): Float = xdpi / 2.54f

    fun pxPerInch(xdpi: Float): Float = xdpi

    /** Normalise an angle difference into (-180, 180]. */
    fun relativeAngle(current: Float, zero: Float): Float {
        var delta = (current - zero) % 360f
        delta = (delta + 360f) % 360f // [0, 360)
        if (delta > 180f) delta -= 360f
        return delta
    }
}
