package com.phongchen.toolbox.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class LocationPermissionMissingException : SecurityException("ACCESS_FINE_LOCATION not granted")

/**
 * Thin wrapper around the framework [LocationManager]: GPS fixes only, no Google Play
 * Services dependency, works offline. Caller must hold ACCESS_FINE_LOCATION.
 */
class LocationProvider(private val context: Context) {

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun fixes(minIntervalMs: Long = 2_000L, minDistanceM: Float = 0f): Flow<Location> = callbackFlow {
        val lm = context.getSystemService(LocationManager::class.java)
        if (!hasPermission) {
            close(LocationPermissionMissingException())
            return@callbackFlow
        }

        // Seed immediately with the last fix if available.
        (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
            ?.let { trySend(it) }

        val listener = LocationListener { location -> trySend(location) }
        lm.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minIntervalMs,
            minDistanceM,
            listener,
            Looper.getMainLooper(),
        )
        awaitClose { lm.removeUpdates(listener) }
    }.conflate()

    /** Satellite counts from the GNSS engine; never emits on devices without a receiver. */
    @SuppressLint("MissingPermission")
    fun gnssStatus(): Flow<GnssInfo> = callbackFlow {
        val lm = context.getSystemService(LocationManager::class.java)
        if (!hasPermission) {
            close(LocationPermissionMissingException())
            return@callbackFlow
        }

        val callback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                var used = 0
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) used++
                }
                trySend(GnssInfo(status.satelliteCount, used))
            }
        }
        val registered = lm.registerGnssStatusCallback(
            callback,
            Handler(Looper.getMainLooper()),
        )
        if (!registered) close(GnssUnavailableException())
        awaitClose { lm.unregisterGnssStatusCallback(callback) }
    }.conflate()
}

data class GnssInfo(val satellites: Int, val usedInFix: Int)

class GnssUnavailableException : RuntimeException("GNSS status callbacks are unavailable")
