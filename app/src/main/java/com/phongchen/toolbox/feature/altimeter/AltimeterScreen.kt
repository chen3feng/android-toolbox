package com.phongchen.toolbox.feature.altimeter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.prefs.ToolSettings
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold

@Composable
fun AltimeterScreen(
    onBack: () -> Unit,
    viewModel: AltimeterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showCalibration by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    ToolScaffold(tool = Tool.ALTIMETER, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            val primary = state.primaryAltitudeM
            val displayValue = primary?.let {
                if (state.metricUnits) it else AltimeterEngine.metersToFeet(it)
            }
            Text(
                text = displayValue?.let { "%.1f".format(it) } ?: "—",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (state.metricUnits) R.string.unit_meter else R.string.unit_feet,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            primary?.let {
                Text(
                    text = stringResource(
                        if (state.usingPressure) R.string.source_pressure else R.string.source_gps,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadingRow(
                        label = stringResource(R.string.pressure_hpa),
                        value = state.pressureHpa?.let { "%.1f hPa".format(it) } ?: "—",
                    )
                    ReadingRow(
                        label = stringResource(R.string.gps_altitude),
                        value = formatGps(state),
                    )
                    ReadingRow(
                        label = "QNH",
                        value = "%.2f hPa".format(state.qnhHpa) +
                            if (state.qnhHpa != ToolSettings.STANDARD_QNH_HPA) {
                                "  (${stringResource(R.string.altimeter_calibrated)})"
                            } else {
                                ""
                            },
                    )
                }
            }

            if (!state.hasLocationPermission && !state.hasPressureSensor) {
                ActionBanner(
                    message = stringResource(R.string.altimeter_location_rationale),
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showCalibration = true }) {
                    Text(stringResource(R.string.common_calibrate))
                }
                OutlinedButton(onClick = { viewModel.setMetricUnits(!state.metricUnits) }) {
                    Text(
                        stringResource(
                            if (state.metricUnits) R.string.unit_feet else R.string.unit_meter,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showCalibration) {
        CalibrationDialog(
            pressureAvailable = state.pressureHpa != null,
            onDismiss = { showCalibration = false },
            onQnh = { viewModel.setQnh(it); showCalibration = false },
            onKnownAltitude = {
                if (viewModel.calibrateToKnownAltitude(it)) showCalibration = false
            },
            onReset = { viewModel.resetStandardQnh(); showCalibration = false },
        )
    }
}

@Composable
private fun formatGps(state: AltimeterUiState): String = when {
    state.gpsAltitudeM != null && state.gpsVerticalAccuracyM != null ->
        "%.1f m  (±%.1f m)".format(state.gpsAltitudeM, state.gpsVerticalAccuracyM)

    state.gpsAltitudeM != null -> "%.1f m".format(state.gpsAltitudeM)
    state.hasLocationPermission -> stringResource(R.string.gps_waiting)
    else -> "—"
}

@Composable
private fun ReadingRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalibrationDialog(
    pressureAvailable: Boolean,
    onDismiss: () -> Unit,
    onQnh: (Double) -> Unit,
    onKnownAltitude: (Double) -> Unit,
    onReset: () -> Unit,
) {
    var qnhText by remember { mutableStateOf("") }
    var knownText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_calibrate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qnhText,
                    onValueChange = { qnhText = it },
                    label = { Text(stringResource(R.string.altimeter_calibrate_qnh)) },
                    placeholder = { Text(stringResource(R.string.altimeter_qnh_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = knownText,
                    onValueChange = { knownText = it },
                    label = { Text(stringResource(R.string.altimeter_calibrate_known)) },
                    placeholder = { Text(stringResource(R.string.altimeter_known_hint)) },
                    singleLine = true,
                    enabled = pressureAvailable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.altimeter_reset_standard))
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { knownText.toDoubleOrNull()?.let(onKnownAltitude) },
                    enabled = pressureAvailable,
                ) { Text(stringResource(R.string.altimeter_apply_known)) }
                TextButton(
                    onClick = { qnhText.toDoubleOrNull()?.let(onQnh) },
                ) { Text(stringResource(R.string.altimeter_apply_qnh)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
