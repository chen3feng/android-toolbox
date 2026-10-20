package com.phongchen.toolbox.feature.gps

import android.app.Application
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.location.GnssUnavailableException
import com.phongchen.toolbox.core.location.LocationPermissionMissingException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GpsUiState(
    val hasLocationPermission: Boolean = false,
    val waitingForFix: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeM: Double? = null,
    val verticalAccuracyM: Float? = null,
    val accuracyM: Float? = null,
    val speedMps: Float? = null,
    val bearing: Float? = null,
    val provider: String? = null,
    val fixTimeMs: Long? = null,
    val satellites: Int? = null,
    val satellitesUsed: Int? = null,
    val metricUnits: Boolean = true,
)

class GpsViewModel(app: Application) : ContainerViewModel(app) {

    private val locationProvider = container.locationProvider
    private var gpsJob: Job? = null
    private var gnssJob: Job? = null

    private val _uiState = MutableStateFlow(
        GpsUiState(hasLocationPermission = locationProvider.hasPermission),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(metricUnits = settings.metricUnits) }
            }
        }
        if (locationProvider.hasPermission) onLocationPermissionChanged(true)
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        gpsJob?.cancel()
        gnssJob?.cancel()
        if (!granted) {
            _uiState.update {
                it.copy(hasLocationPermission = false, waitingForFix = true)
            }
            return
        }
        _uiState.update { it.copy(hasLocationPermission = true) }
        gpsJob = viewModelScope.launch {
            locationProvider.fixes(minIntervalMs = 1_000L)
                .catch { error ->
                    if (error is LocationPermissionMissingException) {
                        _uiState.update { it.copy(hasLocationPermission = false) }
                    }
                }
                .collect { location -> emitFix(location) }
        }
        gnssJob = viewModelScope.launch {
            locationProvider.gnssStatus()
                .catch { error ->
                    if (error is GnssUnavailableException ||
                        error is LocationPermissionMissingException
                    ) {
                        // Satellite counts are optional; just leave the rows hidden.
                    }
                }
                .collect { gnss ->
                    _uiState.update {
                        it.copy(satellites = gnss.satellites, satellitesUsed = gnss.usedInFix)
                    }
                }
        }
    }

    private fun emitFix(location: Location) {
        _uiState.update {
            it.copy(
                waitingForFix = false,
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeM = if (location.hasAltitude()) location.altitude else null,
                verticalAccuracyM = if (location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters
                } else {
                    null
                },
                accuracyM = if (location.hasAccuracy()) location.accuracy else null,
                speedMps = if (location.hasSpeed()) location.speed else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                provider = location.provider,
                fixTimeMs = location.time,
            )
        }
    }
}
