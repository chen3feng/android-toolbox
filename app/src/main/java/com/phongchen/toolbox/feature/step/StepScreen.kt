package com.phongchen.toolbox.feature.step

import android.Manifest
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel

@Composable
fun StepScreen(
    onBack: () -> Unit,
    viewModel: StepViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionChanged(granted)
    }

    ToolScaffold(tool = Tool.STEP, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_step),
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
                text = stringResource(R.string.step_steps),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "%d".format(state.steps),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val distance = if (state.metricUnits) {
                    "%.2f %s".format(
                        state.distanceMeters * PedometerEngine.M_TO_KM,
                        stringResource(R.string.unit_km),
                    )
                } else {
                    "%.2f %s".format(
                        state.distanceMeters * PedometerEngine.M_TO_MILE,
                        stringResource(R.string.unit_mile),
                    )
                }
                StatCard(stringResource(R.string.step_distance), distance, Modifier.weight(1f))
                StatCard(
                    stringResource(R.string.step_kcal),
                    "%.0f".format(state.kcal),
                    Modifier.weight(1f),
                )
                val stepLength = if (state.metricUnits) {
                    "%d cm".format((PedometerEngine.STEP_LENGTH_M * 100).toInt())
                } else {
                    "%.1f ft".format(PedometerEngine.STEP_LENGTH_M * 3.28084f)
                }
                StatCard(stringResource(R.string.step_step_length), stepLength, Modifier.weight(1f))
            }

            if (!state.hasPermission) {
                ActionBanner(
                    message = stringResource(R.string.step_permission_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                )
            }

            Text(
                text = stringResource(R.string.step_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Text(
                    text = stringResource(R.string.step_reset_session),
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
