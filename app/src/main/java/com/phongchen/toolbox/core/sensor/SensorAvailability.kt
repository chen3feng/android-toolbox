package com.phongchen.toolbox.core.sensor

import android.hardware.Sensor
import android.hardware.SensorManager

/** Snapshot of which tool-relevant sensors exist on this device. */
class SensorAvailability(private val sensorManager: SensorManager) {

    private fun has(type: Int): Boolean = sensorManager.getDefaultSensor(type) != null

    /** Rotation vector, or the legacy accelerometer + magnetometer pair. */
    val hasCompass: Boolean =
        has(Sensor.TYPE_ROTATION_VECTOR) ||
            (has(Sensor.TYPE_ACCELEROMETER) && has(Sensor.TYPE_MAGNETIC_FIELD))

    val hasLight: Boolean = has(Sensor.TYPE_LIGHT)
    val hasPressure: Boolean = has(Sensor.TYPE_PRESSURE)

    /** Gravity sensor preferred, accelerometer is an acceptable fallback for tilt. */
    val hasLevel: Boolean = has(Sensor.TYPE_GRAVITY) || has(Sensor.TYPE_ACCELEROMETER)

    val hasMagnetometer: Boolean = has(Sensor.TYPE_MAGNETIC_FIELD)

    /** Hardware step counter preferred; the one-shot detector is an acceptable fallback. */
    val hasStepCounter: Boolean =
        has(Sensor.TYPE_STEP_COUNTER) || has(Sensor.TYPE_STEP_DETECTOR)

    /** Dedicated linear-acceleration sensor preferred, plain accelerometer works too. */
    val hasVibration: Boolean =
        has(Sensor.TYPE_LINEAR_ACCELERATION) || has(Sensor.TYPE_ACCELEROMETER)

    companion object {
        /** TYPE_GRAVITY when present (cleaner tilt), otherwise the accelerometer. */
        fun tiltSensorType(sensorManager: SensorManager): Int =
            if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
                Sensor.TYPE_GRAVITY
            } else {
                Sensor.TYPE_ACCELEROMETER
            }
    }
}
