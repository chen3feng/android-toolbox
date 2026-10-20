package com.phongchen.toolbox.core.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** A sensor reading plus the latest reported accuracy (one of SensorManager.SENSOR_STATUS_*). */
data class SensorSample(
    val values: FloatArray,
    val accuracy: Int,
    val timestampNanos: Long,
)

class SensorMissingException(type: Int) : RuntimeException("Sensor type $type is not available on this device")

/**
 * Cold flow of sensor events. Starts listening on collection and unregisters on cancellation.
 * Fails with [SensorMissingException] if the device has no sensor of [type].
 */
fun SensorManager.sensorFlow(
    type: Int,
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
): Flow<SensorSample> = callbackFlow {
    val sensor = getDefaultSensor(type)
        ?: run {
            close(SensorMissingException(type))
            return@callbackFlow
        }

    var accuracy = SensorManager.SENSOR_STATUS_NO_CONTACT
    var lastValues: FloatArray? = null

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val snapshot = event.values.copyOf()
            lastValues = snapshot
            trySend(SensorSample(snapshot, accuracy, event.timestamp))
        }

        override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
            accuracy = newAccuracy
            lastValues?.let { trySend(SensorSample(it, newAccuracy, System.nanoTime())) }
        }
    }

    registerListener(listener, sensor, samplingPeriodUs)
    awaitClose { unregisterListener(listener) }
}.conflate()
