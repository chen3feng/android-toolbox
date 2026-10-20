package com.phongchen.toolbox.feature.vibration

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.SensorMissingException
import com.phongchen.toolbox.core.sensor.sensorFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VibrationUiState(
    val supported: Boolean = true,
    val magnitude: Float = 0f,
    val peak: Float = 0f,
    val level: Int = 0,
    val waveform: List<Float> = emptyList(),
    val usingLinearSensor: Boolean = true,
) {
    /** Ring buffer for the last few seconds of samples. */
    val waveformReady: Boolean get() = waveform.size >= 2
}

class VibrationViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val sensorType = linearSensor?.type ?: Sensor.TYPE_ACCELEROMETER
    private val buffer = java.util.ArrayDeque<Float>(VibrationEngine.WAVEFORM_CAPACITY)

    private var gravity: FloatArray? = null
    private var peak = 0f

    private val _uiState = MutableStateFlow(
        VibrationUiState(
            supported = container.sensorAvailability.hasVibration,
            usingLinearSensor = linearSensor != null,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (container.sensorAvailability.hasVibration) {
            viewModelScope.launch {
                sensorManager.sensorFlow(sensorType, SensorManager.SENSOR_DELAY_GAME)
                    .catch { error ->
                        if (error is SensorMissingException) {
                            _uiState.update { it.copy(supported = false) }
                        }
                    }
                    .collect { sample ->
                        val vibration = if (linearSensor != null) {
                            VibrationEngine.magnitude(sample.values)
                        } else {
                            val estimate = gravity ?: sample.values.copyOf()
                            gravity = VibrationEngine.lowPass(estimate, sample.values, GRAVITY_ALPHA)
                            VibrationEngine.magnitude(
                                FloatArray(sample.values.size) { i ->
                                    sample.values[i] - (gravity ?: estimate)[i]
                                },
                            )
                        }
                        if (vibration > peak) peak = vibration
                        buffer.addLast(vibration)
                        while (buffer.size > VibrationEngine.WAVEFORM_CAPACITY) {
                            buffer.removeFirst()
                        }
                        _uiState.update {
                            it.copy(
                                magnitude = vibration,
                                peak = peak,
                                level = VibrationEngine.level(vibration),
                                waveform = buffer.toList(),
                            )
                        }
                    }
            }
        }
    }

    fun reset() {
        peak = 0f
        buffer.clear()
        _uiState.update { it.copy(peak = 0f, waveform = emptyList()) }
    }

    private companion object {
        const val GRAVITY_ALPHA = 0.8f
    }
}
