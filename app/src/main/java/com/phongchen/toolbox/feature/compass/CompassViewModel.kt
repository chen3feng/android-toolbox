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
import com.phongchen.toolbox.feature.astro.AstroEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
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
    // --- Sun orientation tab ---
    val hasGameRotation: Boolean = false,
    val sunModeActive: Boolean = false,
    val sunAzimuthDeg: Double? = null,
    val sunAltitudeDeg: Double? = null,
    val sunCalibrated: Boolean = false,
    /** Gyro-anchored heading after the "phone points at the sun" calibration. */
    val sunHeadingDeg: Float? = null,
)

class CompassViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val settingsRepository = container.settingsRepository
    private val locationProvider = container.locationProvider

    private var settings = ToolSettings.DEFAULT
    private var smoothedMagnetic = 0f
    private var declination: Float? = null
    private var locationJob: Job? = null
    private var lastLocation: Location? = null

    // Sun-orientation mode (gyro-only, no magnetometer).
    private var sunJobs: List<Job> = emptyList()
    private var smoothedGame = 0f
    private var gamePrimed = false
    private var anchorYaw: Float? = null
    private var anchorSunAz: Double? = null

    private val _uiState = MutableStateFlow(
        CompassUiState(
            supported = container.sensorAvailability.hasCompass,
            hasLocationPermission = locationProvider.hasPermission,
            hasGameRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null,
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

    /**
     * Starts/stops the sun-orientation mode: the game rotation vector (gyro + gravity,
     * no magnetometer) tracks yaw relative to an arbitrary origin, and a once-per-second
     * tick computes the current Sun azimuth/altitude from the latest GPS fix.
     */
    fun setSunMode(active: Boolean) {
        sunJobs.forEach { it.cancel() }
        sunJobs = emptyList()
        anchorYaw = null
        anchorSunAz = null
        gamePrimed = false
        if (!active) {
            _uiState.update {
                it.copy(
                    sunModeActive = false,
                    sunCalibrated = false,
                    sunHeadingDeg = null,
                    sunAzimuthDeg = null,
                    sunAltitudeDeg = null,
                )
            }
            return
        }
        _uiState.update { it.copy(sunModeActive = true, sunCalibrated = false, sunHeadingDeg = null) }
        val gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (gameSensor != null) {
            sunJobs = sunJobs + viewModelScope.launch {
                sensorManager.sensorFlow(Sensor.TYPE_GAME_ROTATION_VECTOR).collect { sample ->
                    val yaw = CompassEngine.azimuthFromRotationVector(sample.values)
                    // Game RV yaw starts from an arbitrary origin; seed instead of slew.
                    smoothedGame = if (gamePrimed) {
                        CompassEngine.lowPass(smoothedGame, yaw, NEW_SAMPLE_WEIGHT)
                    } else {
                        gamePrimed = true
                        yaw
                    }
                    emitSunHeading()
                }
            }
        }
        sunJobs = sunJobs + viewModelScope.launch {
            flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    delay(1_000L)
                }
            }.collect { now -> updateSun(now) }
        }
    }

    /** Pin the current phone yaw to the Sun azimuth; the device must point at the Sun. */
    fun calibrateSunHeading() {
        val az = _uiState.value.sunAzimuthDeg ?: return
        if ((_uiState.value.sunAltitudeDeg ?: Double.NaN) <= 0.0) return
        anchorYaw = smoothedGame
        anchorSunAz = az
        emitSunHeading()
    }

    fun clearSunCalibration() {
        anchorYaw = null
        anchorSunAz = null
        _uiState.update { it.copy(sunCalibrated = false, sunHeadingDeg = null) }
    }

    private fun updateSun(now: Long) {
        val location = lastLocation ?: return
        val sun = AstroEngine.sunPosition(now, location.latitude, location.longitude)
        _uiState.update { it.copy(sunAzimuthDeg = sun.azimuthDeg, sunAltitudeDeg = sun.altitudeDeg) }
        emitSunHeading()
    }

    private fun emitSunHeading() {
        val baseYaw = anchorYaw ?: return
        val baseAz = anchorSunAz ?: return
        val heading = CompassEngine.normalize(baseAz.toFloat() + (smoothedGame - baseYaw))
        _uiState.update { it.copy(sunCalibrated = true, sunHeadingDeg = heading) }
    }

    private fun updateDeclination(location: Location) {
        lastLocation = location
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
