package com.phongchen.toolbox.feature.sound

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.findActivity
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel

@Composable
fun SoundScreen(
    onBack: () -> Unit,
    viewModel: SoundViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showCalibration by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onMicPermissionChanged(granted)
    }

    val lifecycleOwner = LocalContext.current.findActivity() as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Composition runs at/after ON_START, so make sure capture starts on first entry.
        viewModel.onStart()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onStop()
        }
    }

    ToolScaffold(tool = Tool.SOUND, onBack = onBack) { padding ->
        if (!state.supported) {
            UnsupportedPanel(
                message = stringResource(R.string.unsupported_sound),
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
                text = stringResource(R.string.sound_level),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.splDb?.let { "%.1f".format(it) } ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.sound_unit_db),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                )
            }
            Text(
                text = if (state.calibrated) {
                    stringResource(R.string.sound_calibrated, state.gainDb)
                } else {
                    stringResource(R.string.sound_approx_note)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (state.calibrated) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            val fraction = state.splDb?.let { SoundMeterEngine.gaugeFraction(it.toDouble()) } ?: 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(12.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.sound_chart_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                    SoundHistoryChart(
                        history = state.history,
                        lineColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.sound_min),
                    value = state.minDb?.let { "%.1f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.sound_avg),
                    value = state.avgDb?.let { "%.1f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.sound_max),
                    value = state.maxDb?.let { "%.1f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                state.micBusy -> Text(
                    text = stringResource(R.string.sound_mic_busy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                state.paused -> Text(
                    text = stringResource(R.string.light_hold),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!state.hasMicPermission) {
                ActionBanner(
                    message = stringResource(R.string.sound_mic_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = viewModel::togglePause,
                    enabled = state.hasMicPermission && !state.micBusy,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        if (state.paused) {
                            stringResource(R.string.light_resume)
                        } else {
                            stringResource(R.string.light_pause)
                        },
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                OutlinedButton(
                    onClick = { showCalibration = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.common_calibrate),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                OutlinedButton(
                    onClick = viewModel::reset,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.common_reset),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }

    if (showCalibration) {
        CalibrationDialog(
            calibrated = state.calibrated,
            hasReading = state.splDb != null,
            onDismiss = { showCalibration = false },
            onApply = { knownSpl ->
                viewModel.calibrate(knownSpl)
                showCalibration = false
            },
            onReset = {
                viewModel.resetCalibration()
                showCalibration = false
            },
        )
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

@Composable
private fun CalibrationDialog(
    calibrated: Boolean,
    hasReading: Boolean,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit,
    onReset: () -> Unit,
) {
    var referenceText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_calibrate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.sound_calibrate_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text(stringResource(R.string.sound_calibrate_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (calibrated) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.sound_reset_calibration))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { referenceText.toFloatOrNull()?.let(onApply) },
                enabled = hasReading,
            ) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
