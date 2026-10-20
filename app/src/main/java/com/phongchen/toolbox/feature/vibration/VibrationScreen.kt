package com.phongchen.toolbox.feature.vibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel

@Composable
fun VibrationScreen(
    onBack: () -> Unit,
    viewModel: VibrationViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(tool = Tool.VIBRATION, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_vibration),
                modifier = Modifier.padding(padding),
            )
            return@ToolScaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.vibration_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.2f".format(state.magnitude),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.vibration_unit),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                )
            }

            val levelColor = when (state.level) {
                3 -> MaterialTheme.colorScheme.error
                2 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
            LinearProgressIndicator(
                progress = { VibrationEngine.gaugeFraction(state.magnitude) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = levelColor,
            )
            Text(
                text = stringResource(
                    when (state.level) {
                        1 -> R.string.vibration_level_1
                        2 -> R.string.vibration_level_2
                        3 -> R.string.vibration_level_3
                        else -> R.string.vibration_level_0
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = levelColor,
            )

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.vibration_waveform),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                    Waveform(
                        samples = state.waveform,
                        lineColor = levelColor,
                        gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.vibration_peak),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "%.2f %s".format(
                                state.peak,
                                stringResource(R.string.vibration_unit),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.vibration_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Text(
                    text = stringResource(R.string.common_reset),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Waveform(
    samples: List<Float>,
    lineColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val ticks = listOf(0, 5, 10, 15, 20)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val labelTextSize = 10.sp.toPx()
        val chartLeft = 42.dp.toPx()
        val chartRight = size.width - 4.dp.toPx()
        val chartTop = 6.dp.toPx()
        val chartBottom = size.height - 6.dp.toPx()
        val chartHeight = chartBottom - chartTop

        fun yFor(value: Float): Float =
            chartBottom - (value / VibrationEngine.MAX_MS2).coerceIn(0f, 1f) * chartHeight

        val paint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSize
            isAntiAlias = true
        }
        ticks.forEach { tick ->
            val y = yFor(tick.toFloat())
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                tick.toString(),
                0f,
                y + labelTextSize / 3f,
                paint,
            )
        }

        if (samples.size < 2) return@Canvas
        val path = Path()
        val stepX = (chartRight - chartLeft) / (VibrationEngine.WAVEFORM_CAPACITY - 1)
        val offset = VibrationEngine.WAVEFORM_CAPACITY - samples.size
        samples.forEachIndexed { index, value ->
            val x = chartLeft + (offset + index) * stepX
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}
