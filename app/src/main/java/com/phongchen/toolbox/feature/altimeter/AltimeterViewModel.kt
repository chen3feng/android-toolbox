package com.phongchen.toolbox.feature.altimeter

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.location.LocationPermissionMissingException
import com.phongchen.toolbox.core.prefs.ToolSettings
import com.phongchen.toolbox.core.sensor.sensorFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AltimeterUiState(
    val hasPressureSensor: Boolean = false,
    val pressureHpa: Float? = null,
    val pressureAltitudeM: Float? = null,
    val gpsAltitudeM: Double? = null,
    val gpsVerticalAccuracyM: Float? = null,
    val hasLocationPermission: Boolean = false,
    val metricUnits: Boolean = true,
    val qnhHpa: Double = ToolSettings.STANDARD_QNH_HPA,
) {
    /** Pressure preferred, GPS fallback. */
    val primaryAltitudeM: Float? get() = pressureAltitudeM ?: gpsAltitudeM?.toFloat()
    val usingPressure: Boolean get() = pressureAltitudeM != null
}

class AltimeterViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val settingsRepository = container.settingsRepository
    private val locationProvider = container.locationProvider

    private var settings = ToolSettings.DEFAULT
    private var pressure: Float? = null
    private var location: Location? = null
    private var gpsJob: Job? = null

    private val _uiState = MutableStateFlow(
        AltimeterUiState(
            hasPressureSensor = container.sensorAvailability.hasPressure,
            hasLocationPermission = locationProvider.hasPermission,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect {
                settings = it
                emitState()
            }
        }
        if (container.sensorAvailability.hasPressure) {
            viewModelScope.launch {
                sensorManager.sensorFlow(
                    Sensor.TYPE_PRESSURE,
                    SensorManager.SENSOR_DELAY_UI,
                ).collect { sample ->
                    pressure = sample.values.firstOrNull()
                    emitState()
                }
            }
        }
        if (locationProvider.hasPermission) onLocationPermissionChanged(true)
    }

    fun setMetricUnits(metric: Boolean) {
        viewModelScope.launch { settingsRepository.setMetricUnits(metric) }
    }

    fun setQnh(hpa: Double) {
        viewModelScope.launch { settingsRepository.setQnh(hpa) }
    }

    /** Calibrate against a known altitude at the current pressure. */
    fun calibrateToKnownAltitude(knownAltitudeMeters: Double): Boolean {
        val p = pressure ?: return false
        return runCatching {
            AltimeterEngine.qnhForKnownAltitude(p, knownAltitudeMeters)
        }.getOrNull()?.let {
            setQnh(it)
            true
        } ?: false
    }

    fun resetStandardQnh() {
        viewModelScope.launch { settingsRepository.resetQnh() }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        gpsJob?.cancel()
        if (!granted) {
            location = null
            _uiState.update { it.copy(hasLocationPermission = false, gpsAltitudeM = null) }
            return
        }
        _uiState.update { it.copy(hasLocationPermission = true) }
        gpsJob = viewModelScope.launch {
            locationProvider.fixes()
                .catch { error ->
                    if (error is LocationPermissionMissingException) {
                        _uiState.update { it.copy(hasLocationPermission = false) }
                    }
                }
                .collect {
                    location = it
                    emitState()
                }
        }
    }

    private fun emitState() {
        val pressureAlt = pressure?.let {
            AltimeterEngine.pressureAltitudeMeters(it, settings.qnhHpa)
        }
        val gpsAlt = location?.takeIf { it.hasAltitude() }?.altitude
        val vAcc = location?.verticalAccuracyM()
        _uiState.update {
            it.copy(
                pressureHpa = pressure,
                pressureAltitudeM = pressureAlt,
                gpsAltitudeM = gpsAlt,
                gpsVerticalAccuracyM = vAcc,
                metricUnits = settings.metricUnits,
                qnhHpa = settings.qnhHpa,
            )
        }
    }

    private fun Location.verticalAccuracyM(): Float? =
        if (hasVerticalAccuracy()) verticalAccuracyMeters else null
}
