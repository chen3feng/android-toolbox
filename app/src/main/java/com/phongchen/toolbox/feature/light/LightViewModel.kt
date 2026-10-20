package com.phongchen.toolbox.feature.light

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.sensorFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LightUiState(
    val supported: Boolean = true,
    val lux: Float? = null,
    val minLux: Float? = null,
    val avgLux: Float? = null,
    val maxLux: Float? = null,
    val paused: Boolean = false,
)

class LightViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager

    private var count = 0L
    private var sum = 0.0
    private var minLux = Float.POSITIVE_INFINITY
    private var maxLux = 0f

    private val _uiState = MutableStateFlow(
        LightUiState(supported = container.sensorAvailability.hasLight),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (container.sensorAvailability.hasLight) {
            viewModelScope.launch {
                sensorManager.sensorFlow(
                    Sensor.TYPE_LIGHT,
                    SensorManager.SENSOR_DELAY_UI,
                )
                    .catch { }
                    .collect { sample ->
                        val state = _uiState.value
                        if (state.paused) return@collect
                        record(sample.values.firstOrNull() ?: return@collect)
                    }
            }
        }
    }

    @Synchronized
    private fun record(lux: Float) {
        count += 1
        sum += lux
        if (lux < minLux) minLux = lux
        if (lux > maxLux) maxLux = lux
        _uiState.update {
            it.copy(
                lux = lux,
                minLux = minLux,
                avgLux = (sum / count).toFloat(),
                maxLux = maxLux,
            )
        }
    }

    fun togglePause() = _uiState.update { it.copy(paused = !it.paused) }

    @Synchronized
    fun reset() {
        count = 0L
        sum = 0.0
        minLux = Float.POSITIVE_INFINITY
        maxLux = 0f
        _uiState.update { it.copy(lux = null, minLux = null, avgLux = null, maxLux = null) }
    }
}
