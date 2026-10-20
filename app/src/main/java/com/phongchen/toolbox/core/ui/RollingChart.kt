package com.phongchen.toolbox.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartPoint(val timeMs: Long, val value: Float)

/**
 * Rolling time-series chart with fixed value range and a fixed time window, so levels stay
 * visually comparable. The x axis always spans the full window even when sampling just started.
 */
@Composable
fun RollingLineChart(
    samples: List<ChartPoint>,
    valueMin: Float,
    valueMax: Float,
    valueTicks: List<Int>,
    leftEdgeLabel: String,
    midLabel: String,
    rightEdgeLabel: String,
    lineColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    windowMs: Long = 60_000L,
    chartHeight: Dp = 150.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        val labelTextSize = 10.sp.toPx()
        val chartLeft = 46.dp.toPx()
        val chartRight = size.width - 4.dp.toPx()
        val chartTop = 6.dp.toPx()
        val chartBottom = size.height - 24.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        val span = valueMax - valueMin

        fun yFor(value: Float): Float =
            chartBottom - ((value - valueMin) / span)
                .coerceIn(0f, 1f) * chartHeight

        val axisPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSize
            isAntiAlias = true
        }
        valueTicks.forEach { tick ->
            val y = yFor(tick.toFloat())
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                tick.toString(),
                0f,
                y + labelTextSize / 3f,
                axisPaint,
            )
        }

        drawContext.canvas.nativeCanvas.apply {
            drawText(leftEdgeLabel, chartLeft, size.height - 4.dp.toPx(), axisPaint)
            drawText(
                midLabel,
                chartLeft + chartWidth / 2f - axisPaint.measureText(midLabel) / 2f,
                size.height - 4.dp.toPx(),
                axisPaint,
            )
            drawText(
                rightEdgeLabel,
                chartRight - axisPaint.measureText(rightEdgeLabel),
                size.height - 4.dp.toPx(),
                axisPaint,
            )
        }

        if (samples.size < 2) return@Canvas

        val nowMs = System.currentTimeMillis()
        val windowStart = nowMs - windowMs

        fun xFor(timeMs: Long): Float =
            chartLeft + ((timeMs - windowStart).toFloat() / windowMs)
                .coerceIn(0f, 1f) * chartWidth

        val linePath = Path()
        val fillPath = Path()
        samples.forEachIndexed { index, sample ->
            val x = xFor(sample.timeMs)
            val y = yFor(sample.value)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, chartBottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        val lastX = xFor(samples.last().timeMs)
        fillPath.lineTo(lastX, chartBottom)
        fillPath.close()

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0.02f)),
                startY = chartTop,
                endY = chartBottom,
            ),
        )
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        val lastY = yFor(samples.last().value)
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = lineColor, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))
    }
}
