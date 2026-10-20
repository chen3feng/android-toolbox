package com.phongchen.toolbox.feature.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_TILT_DEG = 30f

@Composable
fun LevelScreen(
    onBack: () -> Unit,
    viewModel: LevelViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(tool = Tool.LEVEL, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_sensor),
                modifier = Modifier.padding(padding),
            )
            return@ToolScaffold
        }
        val tilt = state.calibrated
        val level = LevelEngine.isLevel(tilt)

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (level) stringResource(R.string.level_level) else " ",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Bullseye(
                pitchDeg = tilt.pitchDeg,
                rollDeg = tilt.rollDeg,
                level = level,
                modifier = Modifier.size(300.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AngleReadout(label = stringResource(R.string.level_pitch), angle = tilt.pitchDeg)
                AngleReadout(label = stringResource(R.string.level_roll), angle = tilt.rollDeg)
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = viewModel::setZero,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.level_zero_set),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
                OutlinedButton(
                    onClick = viewModel::resetZero,
                    enabled = state.zeroApplied,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.level_zero_reset),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AngleReadout(label: String, angle: Float) {
    // Avoid a literal "-0.0" when a tiny residual tilt rounds to zero.
    val displayAngle = if (abs(angle) < 0.05f) 0f else angle
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "%+.1f°".format(displayAngle),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Bullseye(
    pitchDeg: Float,
    rollDeg: Float,
    level: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val levelColor = MaterialTheme.colorScheme.primary
    val bubbleColor = if (level) levelColor else MaterialTheme.colorScheme.error

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringRadius = size.minDimension / 2f - 8f
        val bubbleRadius = ringRadius * 0.16f
        val travel = ringRadius - bubbleRadius - 6f

        drawCircle(
            color = onSurfaceVariant.copy(alpha = 0.5f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 4f),
        )
        // Crosshair.
        drawLine(
            onSurfaceVariant.copy(alpha = 0.35f),
            start = Offset(center.x - ringRadius, center.y),
            end = Offset(center.x + ringRadius, center.y),
            strokeWidth = 2f,
        )
        drawLine(
            onSurfaceVariant.copy(alpha = 0.35f),
            start = Offset(center.x, center.y - ringRadius),
            end = Offset(center.x, center.y + ringRadius),
            strokeWidth = 2f,
        )
        // Level tolerance target.
        val targetRadius = travel * (LevelEngine.LEVEL_TOLERANCE_DEG / MAX_TILT_DEG) + bubbleRadius
        drawCircle(
            color = primary.copy(alpha = if (level) 0.25f else 0.08f),
            radius = targetRadius,
            center = center,
        )

        // Bubble runs toward the raised side: opposite to the dipping edge.
        val dx = -(rollDeg / MAX_TILT_DEG).coerceIn(-1f, 1f) * travel
        val dy = (pitchDeg / MAX_TILT_DEG).coerceIn(-1f, 1f) * travel
        val bubbleCenter = Offset(center.x + dx, center.y + dy)
        drawCircle(color = bubbleColor, radius = bubbleRadius, center = bubbleCenter)
        drawLine(
            color = androidx.compose.ui.graphics.Color.White,
            start = Offset(bubbleCenter.x - bubbleRadius * 0.5f, bubbleCenter.y),
            end = Offset(bubbleCenter.x + bubbleRadius * 0.5f, bubbleCenter.y),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = androidx.compose.ui.graphics.Color.White,
            start = Offset(bubbleCenter.x, bubbleCenter.y - bubbleRadius * 0.5f),
            end = Offset(bubbleCenter.x, bubbleCenter.y + bubbleRadius * 0.5f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}
