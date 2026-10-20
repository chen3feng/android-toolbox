package com.phongchen.toolbox.feature.speed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold

@Composable
fun SpeedScreen(
    onBack: () -> Unit,
    viewModel: SpeedViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    ToolScaffold(tool = Tool.SPEED, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            val currentSpeed = state.currentMps?.let {
                if (state.metricUnits) SpeedEngine.kmh(it) else SpeedEngine.mph(it)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentSpeed?.let { it.toInt().coerceAtLeast(0).toString() } ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (state.metricUnits) R.string.speed_unit_kmh else R.string.speed_unit_mph,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                )
            }

            val gaugeMax = if (state.metricUnits) GAUGE_MAX_KMH else GAUGE_MAX_MPH
            LinearProgressIndicator(
                progress = { ((currentSpeed ?: 0f) / gaugeMax).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
            )

            if (state.hasLocationPermission && state.waitingForFix) {
                Text(
                    text = stringResource(R.string.gps_waiting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val avg = state.avgMps.let {
                    if (state.metricUnits) SpeedEngine.kmh(it) else SpeedEngine.mph(it)
                }
                val max = state.maxMps.let {
                    if (state.metricUnits) SpeedEngine.kmh(it) else SpeedEngine.mph(it)
                }
                val trip = if (state.metricUnits) {
                    "%.2f %s".format(SpeedEngine.km(state.tripMeters), stringResource(R.string.unit_km))
                } else {
                    "%.2f %s".format(
                        SpeedEngine.miles(state.tripMeters),
                        stringResource(R.string.unit_mile),
                    )
                }
                StatCard(
                    label = stringResource(R.string.speed_avg),
                    value = if (state.avgMps > 0f) avg.toInt().toString() else "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.speed_max),
                    value = if (state.maxMps > 0f) max.toInt().toString() else "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.speed_trip),
                    value = trip,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!state.hasLocationPermission) {
                ActionBanner(
                    message = stringResource(R.string.speed_location_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                )
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.setMetricUnits(!state.metricUnits) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Text(
                        text = stringResource(
                            if (state.metricUnits) R.string.speed_unit_mph else R.string.speed_unit_kmh,
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Text(
                        text = stringResource(R.string.common_reset),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
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
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private const val GAUGE_MAX_KMH = 120f
private const val GAUGE_MAX_MPH = 80f
