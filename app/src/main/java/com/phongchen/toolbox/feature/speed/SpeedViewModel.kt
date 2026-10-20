package com.phongchen.toolbox.feature.speed

import android.app.Application
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.location.LocationPermissionMissingException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeedUiState(
    val currentMps: Float? = null,
    val avgMps: Float = 0f,
    val maxMps: Float = 0f,
    val tripMeters: Float = 0f,
    val metricUnits: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val waitingForFix: Boolean = true,
)

class SpeedViewModel(app: Application) : ContainerViewModel(app) {

    private val settingsRepository = container.settingsRepository
    private val locationProvider = container.locationProvider

    private var gpsJob: Job? = null
    private var previousFix: Location? = null
    private val accumulator = SpeedAccumulator()

    private val _uiState = MutableStateFlow(
        SpeedUiState(hasLocationPermission = locationProvider.hasPermission),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(metricUnits = settings.metricUnits) }
            }
        }
        if (locationProvider.hasPermission) onLocationPermissionChanged(true)
    }

    fun setMetricUnits(metric: Boolean) {
        viewModelScope.launch { settingsRepository.setMetricUnits(metric) }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        gpsJob?.cancel()
        previousFix = null
        if (!granted) {
            _uiState.update { it.copy(hasLocationPermission = false, waitingForFix = true) }
            return
        }
        _uiState.update { it.copy(hasLocationPermission = true, waitingForFix = true) }
        gpsJob = viewModelScope.launch {
            locationProvider.fixes(minIntervalMs = 1_000L)
                .catch { error ->
                    if (error is LocationPermissionMissingException) {
                        _uiState.update { it.copy(hasLocationPermission = false) }
                    }
                }
                .collect { fix -> onFix(fix) }
        }
    }

    fun reset() {
        accumulator.reset()
        // Keep the current speed reading, but restart the trip from here.
        previousFix = null
        _uiState.update { it.copy(avgMps = 0f, maxMps = 0f, tripMeters = 0f) }
    }

    private fun onFix(fix: Location) {
        val prev = previousFix
        previousFix = fix

        val speed = when {
            fix.hasSpeed() && fix.speed >= 0f -> fix.speed
            prev != null -> {
                val dtMs = fix.time - prev.time
                if (dtMs in 1..10_000L) fix.distanceTo(prev) / (dtMs / 1000f) else 0f
            }
            else -> 0f
        }

        // Only accumulate distance between two recent, accurate fixes, and reject implausible
        // jumps (e.g. the first fix vs a stale last-known location thousands of km away).
        val dtSec = if (prev != null) (fix.time - prev.time) / 1000f else 0f
        val rawDistance = if (prev != null) fix.distanceTo(prev).coerceAtLeast(0f) else 0f
        val distance = if (prev != null &&
            prev.accuracy <= MAX_ACCURACY_M &&
            fix.accuracy <= MAX_ACCURACY_M &&
            dtSec in 0f..MAX_INTERVAL_SEC &&
            rawDistance <= dtSec * MAX_PLAUSIBLE_MPS + JUMP_MARGIN_M
        ) {
            rawDistance
        } else {
            0f
        }

        accumulator.add(speed, distance)
        _uiState.update {
            it.copy(
                currentMps = speed,
                avgMps = accumulator.averageMps,
                maxMps = accumulator.maxMps,
                tripMeters = accumulator.tripMeters,
                waitingForFix = false,
            )
        }
    }

    private companion object {
        const val MAX_ACCURACY_M = 20f
        const val MAX_INTERVAL_SEC = 10f
        const val MAX_PLAUSIBLE_MPS = 100f // 360 km/h upper bound for per-segment movement
        const val JUMP_MARGIN_M = 100f
    }
}
