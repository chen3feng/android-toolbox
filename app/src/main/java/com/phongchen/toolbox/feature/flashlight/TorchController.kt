package com.phongchen.toolbox.feature.flashlight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface TorchState {
    data object Unavailable : TorchState
    data object Off : TorchState
    data object On : TorchState
}

/**
 * App-scoped controller so the LED keeps its state while navigating between screens.
 * [state] follows the real torch mode via [CameraManager.TorchCallback], including
 * changes made outside the app.
 */
class TorchController(context: Context) {

    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)

    private val flashCameraId: String? = run {
        runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private val _state = MutableStateFlow(if (flashCameraId != null) TorchState.Off else TorchState.Unavailable)
    val state: StateFlow<TorchState> = _state.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                _state.value = if (enabled) TorchState.On else TorchState.Off
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == flashCameraId) _state.value = TorchState.Unavailable
        }
    }

    init {
        runCatching { cameraManager.registerTorchCallback(torchCallback, null) }
    }

    val isAvailable: Boolean get() = flashCameraId != null

    /**
     * Sets the LED. Returns false when the camera is busy / denied / missing, so the UI
     * can surface a message instead of pretending it worked.
     */
    fun setTorch(enabled: Boolean): Boolean {
        val id = flashCameraId ?: return false
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        return runCatching {
            cameraManager.setTorchMode(id, enabled)
            true
        }.getOrDefault(false)
    }
}
