package com.phongchen.toolbox.feature.astro

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.location.LocationPermissionMissingException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AstroUiState(
    val hasLocationPermission: Boolean = false,
    val waitingForFix: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Time tick driving the solar clock; null until a fix arrives.
    val nowEpochMs: Long? = null,
    val meanSolarSeconds: Double? = null,
    val apparentSolarSeconds: Double? = null,
    val equationOfTimeSeconds: Double? = null,
    val sunTimes: AstroEngine.SunTimes? = null,
    val moon: AstroEngine.MoonInfo? = null,
    val localDate: LocalDate? = null,
)

/** Emits once immediately and again every [periodMs]; drives the once-per-second redraw. */
private fun ticker(periodMs: Long = 1_000L) = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(periodMs)
    }
}

class AstroViewModel(app: Application) : ContainerViewModel(app) {

    private val locationProvider = container.locationProvider
    private val zone: ZoneId = ZoneId.systemDefault()
    private var locationJob: Job? = null
    private var tickerJob: Job? = null

    @Volatile
    private var lastLat: Double? = null

    @Volatile
    private var lastLon: Double? = null

    private val _uiState = MutableStateFlow(
        AstroUiState(hasLocationPermission = locationProvider.hasPermission),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (locationProvider.hasPermission) onLocationPermissionChanged(true)
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        locationJob?.cancel()
        tickerJob?.cancel()
        if (!granted) {
            lastLat = null
            lastLon = null
            _uiState.update {
                it.copy(hasLocationPermission = false, waitingForFix = true, nowEpochMs = null)
            }
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
                .collect { location ->
                    lastLat = location.latitude
                    lastLon = location.longitude
                    _uiState.update {
                        it.copy(
                            waitingForFix = false,
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    }
                }
        }
        tickerJob = viewModelScope.launch {
            ticker().collect { now -> recompute(now) }
        }
    }

    private fun recompute(now: Long) {
        val lat = lastLat ?: return
        val lon = lastLon ?: return
        val solar = AstroEngine.solarTimes(now, lon)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val previous = _uiState.value
        // Sunrise/sunset only depend on date + position; reuse when unchanged.
        val sunTimes = if (previous.sunTimes != null && previous.localDate == today &&
            previous.latitude == lat && previous.longitude == lon
        ) {
            previous.sunTimes
        } else {
            AstroEngine.sunTimes(today, lat, lon)
        }
        _uiState.update {
            it.copy(
                nowEpochMs = now,
                meanSolarSeconds = solar.meanSecondsOfDay,
                apparentSolarSeconds = solar.apparentSecondsOfDay,
                equationOfTimeSeconds = solar.equationOfTimeSeconds,
                sunTimes = sunTimes,
                moon = AstroEngine.moonInfo(now),
                localDate = today,
            )
        }
    }
}
