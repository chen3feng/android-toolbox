package com.phongchen.toolbox.feature.light

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel
import kotlin.math.ln

@Composable
fun LightScreen(
    onBack: () -> Unit,
    viewModel: LightViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(tool = Tool.LIGHT, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_light),
                modifier = Modifier.padding(padding),
            )
            return@ToolScaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.light_lux),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.lux?.let(::formatLux) ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.light_unit_lx),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                )
            }
            if (state.paused) {
                Text(
                    text = stringResource(R.string.light_hold),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Logarithmic gauge, ~0.01 lx (dark) to 100 000 lx (direct sun).
            val fraction = state.lux?.let { (ln(it + 1.0) / ln(100_001.0)).toFloat() } ?: 0f
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.light_min),
                    value = state.minLux?.let(::formatLux) ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.light_avg),
                    value = state.avgLux?.let(::formatLux) ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.light_max),
                    value = state.maxLux?.let(::formatLux) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::togglePause) {
                    Icon(
                        if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                    )
                    Text(
                        if (state.paused) {
                            stringResource(R.string.light_resume)
                        } else {
                            stringResource(R.string.light_pause)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = viewModel::reset) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Text(
                        stringResource(R.string.common_reset),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatLux(lux: Float): String = when {
    lux < 10f -> "%.2f".format(lux)
    lux < 100f -> "%.1f".format(lux)
    else -> lux.toInt().toString()
}
