package com.phongchen.toolbox.feature.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.findActivity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioScreen(
    onBack: () -> Unit,
    viewModel: AudioViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalContext.current.findActivity() as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onScreenStopped()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenStopped()
        }
    }

    ToolScaffold(tool = Tool.AUDIO, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == AudioToolMode.METRONOME,
                    onClick = { viewModel.setMode(AudioToolMode.METRONOME) },
                    label = { Text(stringResource(R.string.audio_metronome)) },
                )
                FilterChip(
                    selected = state.mode == AudioToolMode.TONE,
                    onClick = { viewModel.setMode(AudioToolMode.TONE) },
                    label = { Text(stringResource(R.string.audio_tone)) },
                )
            }

            if (state.mode == AudioToolMode.METRONOME) {
                Text(
                    text = "%d".format(state.bpm),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.audio_bpm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.bpm.toFloat(),
                    onValueChange = { viewModel.setBpm(it.toInt()) },
                    valueRange = 40f..240f,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { viewModel.setBeats(state.beatsPerMeasure - 1) }) {
                        Text("−")
                    }
                    Text(
                        text = "%d/%d".format(state.beatsPerMeasure, 4),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    OutlinedButton(onClick = { viewModel.setBeats(state.beatsPerMeasure + 1) }) {
                        Text("+")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(state.beatsPerMeasure) { i ->
                        val active = state.playing && state.currentBeat == i
                        Box(
                            modifier = Modifier
                                .size(if (active) 22.dp else 16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        active && i == 0 -> MaterialTheme.colorScheme.error
                                        active -> MaterialTheme.colorScheme.primary
                                        i == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                }

                OutlinedButton(onClick = viewModel::tapTempo) {
                    Text(stringResource(R.string.audio_tap_tempo))
                }
            } else {
                Text(
                    text = "%d Hz".format(state.frequencyHz.toInt()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = state.sliderPosition,
                    onValueChange = viewModel::setFrequencyBySlider,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(256.0, 440.0, 512.0, 1000.0, 2000.0, 4000.0).forEach { freq ->
                        FilterChip(
                            selected = kotlin.math.abs(state.frequencyHz - freq) < 1.0,
                            onClick = { viewModel.setFrequency(freq) },
                            label = { Text("%d".format(freq.toInt())) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaveformType.entries.forEach { type ->
                        FilterChip(
                            selected = state.waveform == type,
                            onClick = { viewModel.setWaveform(type) },
                            label = {
                                Text(
                                    stringResource(
                                        when (type) {
                                            WaveformType.SINE -> R.string.audio_wave_sine
                                            WaveformType.SQUARE -> R.string.audio_wave_square
                                            WaveformType.SAWTOOTH -> R.string.audio_wave_saw
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.audio_volume),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.volume,
                    onValueChange = viewModel::setVolume,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.audio_hearing_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = viewModel::togglePlay,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(
                    if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (state.playing) R.string.audio_stop else R.string.audio_play,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
