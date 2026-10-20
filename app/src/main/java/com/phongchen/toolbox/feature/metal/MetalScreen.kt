package com.phongchen.toolbox.feature.metal

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.RollingLineChart
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel

@Composable
fun MetalScreen(
    onBack: () -> Unit,
    viewModel: MetalViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(tool = Tool.METAL, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_metal),
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
                text = stringResource(R.string.metal_field),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.magnitudeUt?.let { "%.0f".format(it) } ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.strong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.metal_unit_ut),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                )
            }

            val valueColor = if (state.strong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            LinearProgressIndicator(
                progress = { state.magnitudeUt?.let { MetalEngine.gaugeFraction(it) } ?: 0f },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = valueColor,
            )
            Text(
                text = stringResource(
                    if (state.strong) R.string.metal_strong else R.string.metal_normal,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (state.strong) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.metal_chart_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                    RollingLineChart(
                        samples = state.history,
                        valueMin = MetalEngine.MIN_UT,
                        valueMax = MetalEngine.MAX_UT,
                        valueTicks = listOf(0, 250, 500, 750, 1000),
                        leftEdgeLabel = "-60s",
                        midLabel = "-30s",
                        rightEdgeLabel = stringResource(R.string.sound_chart_now),
                        lineColor = valueColor,
                        gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(stringResource(R.string.metal_min), state.minUt?.let { "%.0f".format(it) } ?: "—", Modifier.weight(1f))
                StatCard(stringResource(R.string.metal_avg), state.avgUt?.let { "%.0f".format(it) } ?: "—", Modifier.weight(1f))
                StatCard(stringResource(R.string.metal_max), state.maxUt?.let { "%.0f".format(it) } ?: "—", Modifier.weight(1f))
            }

            Text(
                text = stringResource(R.string.metal_note),
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
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
