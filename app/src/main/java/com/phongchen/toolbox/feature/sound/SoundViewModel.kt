package com.phongchen.toolbox.feature.sound

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_MICROPHONE
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.prefs.ToolSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoundSample(val timeMs: Long, val splDb: Float)

data class SoundUiState(
    val supported: Boolean = true,
    val hasMicPermission: Boolean = false,
    val recording: Boolean = false,
    val micBusy: Boolean = false,
    val paused: Boolean = false,
    val splDb: Float? = null,
    val minDb: Float? = null,
    val avgDb: Float? = null,
    val maxDb: Float? = null,
    val gainDb: Float = ToolSettings.DEFAULT_SOUND_GAIN_DB,
    val history: List<SoundSample> = emptyList(),
) {
    val calibrated: Boolean get() = gainDb != ToolSettings.DEFAULT_SOUND_GAIN_DB
}

class SoundViewModel(app: Application) : ContainerViewModel(app) {

    private val settingsRepository = container.settingsRepository
    private val recorder = MicRecorder(app)

    private var gainDb: Float = ToolSettings.DEFAULT_SOUND_GAIN_DB
    private var smoothedDbFs: Double? = null
    private var count = 0L
    private var sum = 0.0
    private var minDb = Float.POSITIVE_INFINITY
    private var maxDb = Float.NEGATIVE_INFINITY
    private val history = java.util.ArrayDeque<SoundSample>()

    private val _uiState = MutableStateFlow(
        SoundUiState(
            supported = app.packageManager.hasSystemFeature(FEATURE_MICROPHONE),
            hasMicPermission = hasMicPermission(),
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                gainDb = settings.soundGainDb
                _uiState.update { it.copy(gainDb = settings.soundGainDb) }
            }
        }
    }

    /** Called from the screen lifecycle; only records while started, permitted and unpaused. */
    fun onStart() {
        val state = _uiState.value
        if (state.hasMicPermission && !state.paused && state.supported) startRecording()
    }

    fun onStop() = stopRecording()

    fun onMicPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasMicPermission = granted, micBusy = false) }
        if (granted) startRecording() else stopRecording()
    }

    fun togglePause() {
        _uiState.update { state ->
            val paused = !state.paused
            if (paused) stopRecording() else if (state.hasMicPermission) startRecording()
            state.copy(paused = paused)
        }
    }

    /** Calibrate so the current level reads [knownSplDb]; requires a live measurement. */
    fun calibrate(knownSplDb: Float) {
        val measured = smoothedDbFs ?: return
        val gain = SoundMeterEngine.calibrationGain(measured, knownSplDb.toDouble()).toFloat()
        viewModelScope.launch { settingsRepository.setSoundGain(gain) }
    }

    fun resetCalibration() {
        viewModelScope.launch { settingsRepository.resetSoundGain() }
    }

    fun reset() {
        count = 0L
        sum = 0.0
        minDb = Float.POSITIVE_INFINITY
        maxDb = Float.NEGATIVE_INFINITY
        history.clear()
        _uiState.update {
            it.copy(
                splDb = null,
                minDb = null,
                avgDb = null,
                maxDb = null,
                history = emptyList(),
            )
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun startRecording() {
        if (recorder.isRecording) return
        _uiState.update { it.copy(micBusy = false, recording = true) }
        recorder.start(
            scope = viewModelScope,
            onWindow = ::onWindow,
            onError = { _uiState.update { it.copy(recording = false, micBusy = true) } },
        )
    }

    private fun stopRecording() {
        recorder.stop()
        _uiState.update { it.copy(recording = false) }
    }

    @Synchronized
    private fun onWindow(samples: ShortArray) {
        val state = _uiState.value
        if (state.paused) return
        val rms = SoundMeterEngine.rms(samples)
        val dbFs = SoundMeterEngine.dbFs(rms)
        // Fast time weighting (~125 ms): 250 ms windows get alpha 0.86.
        val smoothed = SoundMeterEngine.smooth(
            previous = smoothedDbFs ?: dbFs,
            current = dbFs,
            newWeight = 0.86,
        )
        smoothedDbFs = smoothed
        val spl = SoundMeterEngine.splDb(smoothed, gainDb.toDouble()).toFloat()

        count += 1
        sum += spl
        if (spl < minDb) minDb = spl
        if (spl > maxDb) maxDb = spl

        val now = System.currentTimeMillis()
        history.addLast(SoundSample(now, spl))
        val cutoff = now - HISTORY_WINDOW_MS
        while (true) {
            val oldest = history.peekFirst() ?: break
            if (oldest.timeMs >= cutoff) break
            history.removeFirst()
        }

        _uiState.update {
            it.copy(
                splDb = spl,
                minDb = minDb,
                avgDb = (sum / count).toFloat(),
                maxDb = maxDb,
                history = history.toList(),
            )
        }
    }

    private companion object {
        const val HISTORY_WINDOW_MS = 60_000L
    }
}
