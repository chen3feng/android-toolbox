package com.phongchen.toolbox.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold

private val COUNTDOWN_PRESETS_MIN = listOf(1, 3, 5, 10, 15)

@Composable
fun TimerScreen(
    onBack: () -> Unit,
    viewModel: TimerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(tool = Tool.TIMER, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == TimerMode.STOPWATCH,
                    onClick = { viewModel.setMode(TimerMode.STOPWATCH) },
                    label = { Text(stringResource(R.string.timer_stopwatch)) },
                )
                FilterChip(
                    selected = state.mode == TimerMode.COUNTDOWN,
                    onClick = { viewModel.setMode(TimerMode.COUNTDOWN) },
                    label = { Text(stringResource(R.string.timer_countdown)) },
                )
            }

            if (state.mode == TimerMode.STOPWATCH) {
                Text(
                    text = TimerEngine.formatElapsed(state.elapsedMs),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.laps.withIndex().toList().asReversed()) { _, entry ->
                        val lap = entry.value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.timer_lap_n, entry.index + 1),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                TimerEngine.formatElapsed(lap),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::stopwatchLapOrReset,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.stopwatchRunning) R.string.timer_lap else R.string.common_reset,
                            ),
                        )
                    }
                    Button(
                        onClick = viewModel::stopwatchStartPause,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(
                            if (state.stopwatchRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Text(
                            stringResource(
                                if (state.stopwatchRunning) R.string.timer_pause else R.string.timer_start,
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            } else {
                val progress = if (state.totalMs > 0) {
                    (state.remainingMs.toFloat() / state.totalMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.height(220.dp).padding(8.dp),
                        strokeWidth = 10.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = TimerEngine.formatCountdown(state.remainingMs),
                            style = MaterialTheme.typography.displayMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (state.countdownFinished) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (state.countdownFinished) {
                            Text(
                                text = stringResource(R.string.timer_finished),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COUNTDOWN_PRESETS_MIN.forEach { minutes ->
                        FilterChip(
                            selected = state.totalMs == minutes * 60_000L &&
                                !state.countdownRunning,
                            onClick = { viewModel.setCountdownMinutes(minutes) },
                            label = { Text("%d′".format(minutes)) },
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::countdownReset,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null)
                        Text(
                            stringResource(R.string.common_reset),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Button(
                        onClick = viewModel::countdownStartPause,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(
                            if (state.countdownRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Text(
                            stringResource(
                                if (state.countdownRunning) R.string.timer_pause else R.string.timer_start,
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
