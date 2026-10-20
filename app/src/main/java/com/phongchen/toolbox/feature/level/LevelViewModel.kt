package com.phongchen.toolbox.feature.level

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.prefs.ToolSettings
import com.phongchen.toolbox.core.sensor.SensorAvailability
import com.phongchen.toolbox.core.sensor.sensorFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LevelUiState(
    val supported: Boolean = true,
    val tilt: Tilt = Tilt(0f, 0f),
    val calibrated: Tilt = Tilt(0f, 0f),
    val zeroApplied: Boolean = false,
)

class LevelViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val sensorType = SensorAvailability.tiltSensorType(sensorManager)

    private var settings = ToolSettings.DEFAULT
    private var rawTilt = Tilt(0f, 0f)

    private val _uiState = MutableStateFlow(
        LevelUiState(supported = container.sensorAvailability.hasLevel),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect {
                settings = it
                emitState()
            }
        }
        viewModelScope.launch {
            sensorManager.sensorFlow(sensorType).collect { sample ->
                rawTilt = LevelEngine.tilt(sample.values)
                emitState()
            }
        }
    }

    /** Zero the gauge at the current orientation. */
    fun setZero() {
        viewModelScope.launch {
            container.settingsRepository.setLevelZero(rawTilt.pitchDeg, rawTilt.rollDeg)
        }
    }

    fun resetZero() {
        viewModelScope.launch { container.settingsRepository.resetLevelZero() }
    }

    private fun emitState() {
        val calibrated = LevelEngine.applyZero(
            rawTilt,
            settings.levelZeroPitch,
            settings.levelZeroRoll,
        )
        _uiState.update {
            it.copy(
                tilt = rawTilt,
                calibrated = calibrated,
                zeroApplied = settings.levelZeroPitch != 0f || settings.levelZeroRoll != 0f,
            )
        }
    }
}
