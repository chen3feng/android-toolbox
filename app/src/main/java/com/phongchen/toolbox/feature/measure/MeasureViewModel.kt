package com.phongchen.toolbox.feature.measure

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.sensor.SensorAvailability
import com.phongchen.toolbox.core.sensor.sensorFlow
import com.phongchen.toolbox.feature.level.LevelEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeasureUiState(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val zeroPitch: Float = 0f,
    /** Plumb-line deviation for an upright phone, atan2(gx, -gy). */
    val plumbDeg: Float = 0f,
    /** False when the phone is lying flat: a plumb line has no meaning on the screen. */
    val plumbValid: Boolean = false,
    val zeroPlumb: Float = 0f,
)

class MeasureViewModel(app: Application) : ContainerViewModel(app) {

    private val sensorManager = container.sensorManager
    private val sensorType = SensorAvailability.tiltSensorType(sensorManager)

    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState = _uiState.asStateFlow()

    private var smoothedPlumb = 0f

    init {
        viewModelScope.launch {
            sensorManager.sensorFlow(sensorType, SensorManager.SENSOR_DELAY_GAME).collect { sample ->
                val tilt = LevelEngine.tilt(sample.values)
                val plumb = uprightPlumb(sample.values)
                smoothedPlumb = if (plumb.second) {
                    // Angle-aware behaviour is unnecessary in the narrow upright range.
                    smoothedPlumb + (plumb.first - smoothedPlumb) * PLUMB_SMOOTH_WEIGHT
                } else {
                    plumb.first
                }
                _uiState.update {
                    it.copy(
                        pitchDeg = tilt.pitchDeg,
                        rollDeg = tilt.rollDeg,
                        plumbDeg = smoothedPlumb,
                        plumbValid = plumb.second,
                    )
                }
            }
        }
    }

    /**
     * Plumb deviation for a phone held upright: the gravity vector projected onto the
     * screen plane, angle = atan2(gx, gy). It reads 0 vertical, positive when the top
     * tilts left. Sign-independent validity (|gy| large) accepts both normal and
     * reverse-portrait grips.
     *
     * The previous atan2(gx, gz) roll is singular when upright — tiny tilts read as
     * tens of degrees — and a face-down phone (gz < 0) still drew a vertical line.
     *
     * @return (degrees, valid) where valid means the screen is roughly vertical.
     */
    private fun uprightPlumb(gravity: FloatArray): Pair<Float, Boolean> {
        val magnitude = kotlin.math.sqrt(
            gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2],
        )
        if (magnitude == 0f) return 0f to false
        val gx = gravity[0] / magnitude
        val gy = gravity[1] / magnitude
        // The bob points at world-down expressed in the phone frame, but when the screen
        // tilts clockwise (right edge down, gx > 0) world-down sits on the frame LEFT:
        // negate x. In reverse portrait (gy < 0) the UI is rotated 180° by the system,
        // flipping both screen axes.
        val downX = if (gy < 0f) gx else -gx
        val downY = kotlin.math.abs(gy)
        val angle = Math.toDegrees(kotlin.math.atan2(downX, downY).toDouble()).toFloat()
        // Meaningful whenever the screen is roughly vertical, either edge up.
        return angle to (downY > 0.4f)
    }

    fun setProtractorZero() {
        _uiState.update { it.copy(zeroPitch = it.pitchDeg) }
    }

    fun setPlumbZero() {
        _uiState.update { it.copy(zeroPlumb = it.plumbDeg) }
    }

    fun resetZero() {
        _uiState.update { it.copy(zeroPitch = 0f, zeroPlumb = 0f) }
    }

    companion object {
        private const val PLUMB_SMOOTH_WEIGHT = 0.2f
    }
}
