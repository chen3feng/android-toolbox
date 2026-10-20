package com.phongchen.toolbox.feature.measure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.feature.level.LevelEngine
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private enum class MeasureMode { RULER, PROTRACTOR, PLUMB }

@Composable
fun MeasureScreen(
    onBack: () -> Unit,
    viewModel: MeasureViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(MeasureMode.RULER) }
    var metric by remember { mutableStateOf(true) }

    ToolScaffold(tool = Tool.MEASURE, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == MeasureMode.RULER,
                    onClick = { mode = MeasureMode.RULER },
                    label = { Text(stringResource(R.string.measure_ruler)) },
                )
                FilterChip(
                    selected = mode == MeasureMode.PROTRACTOR,
                    onClick = { mode = MeasureMode.PROTRACTOR },
                    label = { Text(stringResource(R.string.measure_protractor)) },
                )
                FilterChip(
                    selected = mode == MeasureMode.PLUMB,
                    onClick = { mode = MeasureMode.PLUMB },
                    label = { Text(stringResource(R.string.measure_plumb)) },
                )
            }

            when (mode) {
                MeasureMode.RULER -> Ruler(
                    metric = metric,
                    onToggleUnit = { metric = !metric },
                    modifier = Modifier.weight(1f),
                )
                MeasureMode.PROTRACTOR -> AngleSurface(
                    angle = MeasureEngine.relativeAngle(state.pitchDeg, state.zeroPitch),
                    onSetZero = viewModel::setZero,
                    onResetZero = viewModel::resetZero,
                    modifier = Modifier.weight(1f),
                ) { angle, lineColor ->
                    ProtractorDial(angle, lineColor)
                }
                MeasureMode.PLUMB -> AngleSurface(
                    angle = MeasureEngine.relativeAngle(state.rollDeg, state.zeroRoll),
                    onSetZero = viewModel::setZero,
                    onResetZero = viewModel::resetZero,
                    modifier = Modifier.weight(1f),
                ) { angle, lineColor ->
                    PlumbLine(angle, lineColor)
                }
            }
        }
    }
}

@Composable
private fun Ruler(metric: Boolean, onToggleUnit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ydpi = context.resources.displayMetrics.ydpi
    val pxPerUnit = if (metric) MeasureEngine.pxPerCm(ydpi) else MeasureEngine.pxPerInch(ydpi)
    val length = if (metric) MeasureEngine.RULER_LENGTH_CM else MeasureEngine.RULER_LENGTH_INCHES
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val centerValue = (scroll.value + viewport.height / 2f) / pxPerUnit
    val labelColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (metric) "%.1f cm".format(centerValue) else "%.2f in".format(centerValue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            FilterChip(
                selected = metric,
                onClick = onToggleUnit,
                label = { Text(if (metric) "cm" else "in") },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { viewport = it },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { (length * pxPerUnit).toInt().toDp() }),
                ) {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                    }
                    if (metric) {
                        val mm = pxPerUnit / 10f
                        for (i in 0..length * 10) {
                            val y = i * mm
                            val tick = when {
                                i % 10 == 0 -> 40.dp.toPx()
                                i % 5 == 0 -> 28.dp.toPx()
                                else -> 16.dp.toPx()
                            }
                            drawLine(tickColor, Offset(0f, y), Offset(tick, y), strokeWidth = 2f)
                            drawLine(
                                tickColor,
                                Offset(size.width, y),
                                Offset(size.width - tick, y),
                                strokeWidth = 2f,
                            )
                            if (i % 10 == 0 && i > 0) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    (i / 10).toString(),
                                    46.dp.toPx(),
                                    y + paint.textSize / 3f,
                                    paint,
                                )
                            }
                        }
                    } else {
                        val sixteenth = pxPerUnit / 16f
                        for (i in 0..length * 16) {
                            val y = i * sixteenth
                            val tick = when {
                                i % 16 == 0 -> 40.dp.toPx()
                                i % 8 == 0 -> 32.dp.toPx()
                                i % 4 == 0 -> 24.dp.toPx()
                                i % 2 == 0 -> 16.dp.toPx()
                                else -> 10.dp.toPx()
                            }
                            drawLine(tickColor, Offset(0f, y), Offset(tick, y), strokeWidth = 2f)
                            drawLine(
                                tickColor,
                                Offset(size.width, y),
                                Offset(size.width - tick, y),
                                strokeWidth = 2f,
                            )
                            if (i % 16 == 0 && i > 0) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    (i / 16).toString(),
                                    46.dp.toPx(),
                                    y + paint.textSize / 3f,
                                    paint,
                                )
                            }
                        }
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(accent, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 3f)
            }
        }
    }
}

@Composable
private fun AngleSurface(
    angle: Float,
    onSetZero: () -> Unit,
    onResetZero: () -> Unit,
    modifier: Modifier = Modifier,
    dial: @Composable (Float, Color) -> Unit,
) {
    val aligned = abs(angle) <= LevelEngine.LEVEL_TOLERANCE_DEG
    val lineColor = if (aligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dial(angle, lineColor)
        Text(
            text = "%.1f°".format(angle),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSetZero) {
                Text(stringResource(R.string.measure_set_zero))
            }
            OutlinedButton(onClick = onResetZero) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Text(stringResource(R.string.common_reset), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ProtractorDial(angle: Float, lineColor: Color) {
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        val cx = size.width / 2f
        val cy = size.height - 20.dp.toPx()
        val radius = minOf(cx, cy) - 8.dp.toPx()
        drawArc(
            color = tickColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 2f),
        )
        for (deg in 0..180 step 10) {
            val rad = Math.toRadians((180 - deg).toDouble())
            val outer = Offset(cx + (radius * cos(rad)).toFloat(), cy - (radius * sin(rad)).toFloat())
            val innerRadius = radius - 14.dp.toPx()
            val inner = Offset(cx + (innerRadius * cos(rad)).toFloat(), cy - (innerRadius * sin(rad)).toFloat())
            drawLine(tickColor, inner, outer, strokeWidth = 2f)
        }
        val needleRad = Math.toRadians((90 - angle.coerceIn(-90f, 90f)).toDouble())
        drawLine(
            lineColor,
            Offset(cx, cy),
            Offset(cx + (radius * cos(needleRad)).toFloat(), cy - (radius * sin(needleRad)).toFloat()),
            strokeWidth = 5f,
        )
    }
}

@Composable
private fun PlumbLine(angle: Float, lineColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .height(320.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val length = size.height - 60.dp.toPx()
            val rad = Math.toRadians(angle.toDouble())
            val tip = Offset(
                cx + (length * sin(rad)).toFloat(),
                length * cos(rad).toFloat(),
            )
            drawLine(lineColor, Offset(cx, 0f), tip, strokeWidth = 5f)
            drawCircle(lineColor, radius = 18.dp.toPx(), center = tip)
        }
        val aligned = abs(angle) <= LevelEngine.LEVEL_TOLERANCE_DEG
        if (aligned) {
            Text(
                text = stringResource(R.string.measure_vertical),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
