package com.phongchen.toolbox.feature.measure

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.SensorAvailability
import com.phongchen.toolbox.core.sensor.sensorFlow
import com.phongchen.toolbox.feature.level.LevelEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeasureUiState(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val zeroPitch: Float = 0f,
    val zeroRoll: Float = 0f,
)

class MeasureViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val sensorType = SensorAvailability.tiltSensorType(sensorManager)

    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sensorManager.sensorFlow(sensorType, SensorManager.SENSOR_DELAY_GAME).collect { sample ->
                val tilt = LevelEngine.tilt(sample.values)
                _uiState.update { it.copy(pitchDeg = tilt.pitchDeg, rollDeg = tilt.rollDeg) }
            }
        }
    }

    fun setZero() {
        _uiState.update { it.copy(zeroPitch = it.pitchDeg, zeroRoll = it.rollDeg) }
    }

    fun resetZero() {
        _uiState.update { it.copy(zeroPitch = 0f, zeroRoll = 0f) }
    }
}
