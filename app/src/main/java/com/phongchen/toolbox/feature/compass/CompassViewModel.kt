package com.phongchen.toolbox.feature.compass

import android.app.Application
import android.hardware.GeomagneticField
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompassUiState(
    val supported: Boolean = true,
    val azimuthDeg: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_NO_CONTACT,
    val trueNorthEnabled: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val declinationDeg: Float? = null,
)

class CompassViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val settingsRepository = container.settingsRepository
    private val locationProvider = container.locationProvider

    private var settings = ToolSettings.DEFAULT
    private var smoothedMagnetic = 0f
    private var declination: Float? = null
    private var locationJob: Job? = null

    private val _uiState = MutableStateFlow(
        CompassUiState(
            supported = container.sensorAvailability.hasCompass,
            hasLocationPermission = locationProvider.hasPermission,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val headings: kotlinx.coroutines.flow.Flow<Pair<Float, Int>> =
        if (rotationSensor != null) {
            sensorManager.sensorFlow(Sensor.TYPE_ROTATION_VECTOR)
                .map { CompassEngine.azimuthFromRotationVector(it.values) to it.accuracy }
        } else {
            combine(
                sensorManager.sensorFlow(Sensor.TYPE_ACCELEROMETER),
                sensorManager.sensorFlow(Sensor.TYPE_MAGNETIC_FIELD),
            ) { accel, mag ->
                CompassEngine.azimuthFromAccelMag(accel.values, mag.values)?.let { it to mag.accuracy }
            }.filterNotNull()
        }

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect {
                settings = it
                emitState()
            }
        }
        viewModelScope.launch {
            headings.collect { (azimuth, accuracy) ->
                smoothedMagnetic = CompassEngine.lowPass(smoothedMagnetic, azimuth, NEW_SAMPLE_WEIGHT)
                _uiState.update { it.copy(accuracy = accuracy) }
                emitState()
            }
        }
        if (locationProvider.hasPermission) onLocationPermissionChanged(true)
    }

    fun setTrueNorth(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrueNorth(enabled) }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        locationJob?.cancel()
        if (!granted) {
            declination = null
            _uiState.update { it.copy(hasLocationPermission = false, declinationDeg = null) }
            return
        }
        _uiState.update { it.copy(hasLocationPermission = true) }
        locationJob = viewModelScope.launch {
            locationProvider.fixes()
                .catch { error ->
                    if (error is LocationPermissionMissingException) {
                        _uiState.update { it.copy(hasLocationPermission = false) }
                    }
                }
                .collect { location -> updateDeclination(location) }
        }
    }

    private fun updateDeclination(location: Location) {
        val altitude = if (location.hasAltitude()) location.altitude.toFloat() else 0f
        val field = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            altitude,
            location.time,
        )
        declination = field.declination
        emitState()
    }

    private fun emitState() {
        val magnetic = CompassEngine.normalize(smoothedMagnetic)
        val display = if (settings.trueNorthEnabled && declination != null) {
            CompassEngine.withDeclination(magnetic, declination!!)
        } else {
            magnetic
        }
        _uiState.update {
            it.copy(
                azimuthDeg = display,
                trueNorthEnabled = settings.trueNorthEnabled,
                declinationDeg = declination,
            )
        }
    }

    companion object {
        private const val NEW_SAMPLE_WEIGHT = 0.18f
    }
}
