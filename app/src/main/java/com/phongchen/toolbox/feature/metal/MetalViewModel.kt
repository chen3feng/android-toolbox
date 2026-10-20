package com.phongchen.toolbox.feature.metal

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.SensorMissingException
import com.phongchen.toolbox.core.sensor.sensorFlow
import com.phongchen.toolbox.core.ui.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetalUiState(
    val supported: Boolean = true,
    val magnitudeUt: Float? = null,
    val minUt: Float? = null,
    val avgUt: Float? = null,
    val maxUt: Float? = null,
    val strong: Boolean = false,
    val history: List<ChartPoint> = emptyList(),
)

class MetalViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val history = java.util.ArrayDeque<ChartPoint>()

    private var smoothed: Float? = null
    private var count = 0L
    private var sum = 0.0
    private var minUt = Float.POSITIVE_INFINITY
    private var maxUt = Float.NEGATIVE_INFINITY

    private val _uiState = MutableStateFlow(
        MetalUiState(supported = container.sensorAvailability.hasMagnetometer),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (container.sensorAvailability.hasMagnetometer) {
            viewModelScope.launch {
                sensorManager.sensorFlow(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_UI)
                    .catch { error ->
                        if (error is SensorMissingException) {
                            _uiState.update { it.copy(supported = false) }
                        }
                    }
                    .collect { sample ->
                        val raw = MetalEngine.magnitudeUt(sample.values)
                        val value = MetalEngine.lowPass(smoothed ?: raw, raw, NEW_SAMPLE_WEIGHT)
                        smoothed = value

                        count += 1
                        sum += value
                        if (value < minUt) minUt = value
                        if (value > maxUt) maxUt = value

                        val now = System.currentTimeMillis()
                        history.addLast(ChartPoint(now, value))
                        val cutoff = now - HISTORY_WINDOW_MS
                        while (true) {
                            val oldest = history.peekFirst() ?: break
                            if (oldest.timeMs >= cutoff) break
                            history.removeFirst()
                        }

                        _uiState.update {
                            it.copy(
                                magnitudeUt = value,
                                minUt = minUt,
                                avgUt = (sum / count).toFloat(),
                                maxUt = maxUt,
                                strong = value >= MetalEngine.STRONG_UT,
                                history = history.toList(),
                            )
                        }
                    }
            }
        }
    }

    fun reset() {
        count = 0L
        sum = 0.0
        minUt = Float.POSITIVE_INFINITY
        maxUt = Float.NEGATIVE_INFINITY
        history.clear()
        smoothed = null
        _uiState.update {
            it.copy(
                magnitudeUt = null,
                minUt = null,
                avgUt = null,
                maxUt = null,
                strong = false,
                history = emptyList(),
            )
        }
    }

    private companion object {
        const val NEW_SAMPLE_WEIGHT = 0.2f
        const val HISTORY_WINDOW_MS = 60_000L
    }
}
