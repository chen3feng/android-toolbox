package com.phongchen.toolbox.feature.sound

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.ui.ChartPoint
import com.phongchen.toolbox.core.ui.RollingLineChart

private val DB_TICKS = listOf(30, 50, 70, 90, 110)

/** Rolling 60-second SPL chart; thin adapter over the shared [RollingLineChart]. */
@Composable
fun SoundHistoryChart(
    history: List<SoundSample>,
    lineColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    RollingLineChart(
        samples = history.map { ChartPoint(it.timeMs, it.splDb) },
        valueMin = SoundMeterEngine.MIN_SPL_DB.toFloat(),
        valueMax = SoundMeterEngine.MAX_SPL_DB.toFloat(),
        valueTicks = DB_TICKS,
        leftEdgeLabel = "-60s",
        midLabel = "-30s",
        rightEdgeLabel = stringResource(R.string.sound_chart_now),
        lineColor = lineColor,
        gridColor = gridColor,
        labelColor = labelColor,
        modifier = modifier,
    )
}
