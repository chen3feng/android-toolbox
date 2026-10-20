package com.phongchen.toolbox.feature.step

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.sensorFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StepUiState(
    val supported: Boolean = true,
    val hasPermission: Boolean = false,
    val steps: Long = 0L,
    val distanceMeters: Float = 0f,
    val kcal: Float = 0f,
    val metricUnits: Boolean = true,
)

class StepViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val counterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private var sensorJob: Job? = null

    // STEP_COUNTER reports a boot-relative cumulative total; baseline is the session start.
    private var baseline: Float? = null
    private var lastTotal: Float = 0f
    private var detectorSteps = 0L

    private val _uiState = MutableStateFlow(
        StepUiState(
            supported = container.sensorAvailability.hasStepCounter,
            hasPermission = hasPermission(),
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(metricUnits = settings.metricUnits) }
            }
        }
        if (hasPermission()) onPermissionChanged(true)
    }

    fun onPermissionChanged(granted: Boolean) {
        sensorJob?.cancel()
        if (!granted) {
            _uiState.update { it.copy(hasPermission = false) }
            return
        }
        _uiState.update { it.copy(hasPermission = true) }
        sensorJob = viewModelScope.launch {
            if (counterSensor != null) {
                sensorManager.sensorFlow(Sensor.TYPE_STEP_COUNTER, SensorManager.SENSOR_DELAY_UI)
                    .collect { onCounter(it.values.firstOrNull() ?: return@collect) }
            } else {
                sensorManager.sensorFlow(Sensor.TYPE_STEP_DETECTOR, SensorManager.SENSOR_DELAY_UI)
                    .collect {
                        detectorSteps += 1L
                        emitSteps(detectorSteps.toFloat())
                    }
            }
        }
    }

    private fun onCounter(total: Float) {
        lastTotal = total
        if (baseline == null) baseline = total
        emitSteps(PedometerEngine.sessionSteps(total, baseline))
    }

    /** Restart the session at the current counter value. */
    fun reset() {
        baseline = lastTotal
        detectorSteps = 0L
        emitSteps(0f)
    }

    private fun emitSteps(steps: Float) {
        _uiState.update {
            it.copy(
                steps = steps.toLong(),
                distanceMeters = PedometerEngine.distanceMeters(steps),
                kcal = PedometerEngine.caloriesKcal(steps),
            )
        }
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED
}
