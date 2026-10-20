package com.phongchen.toolbox.core

import android.content.Context
import android.hardware.SensorManager
import com.phongchen.toolbox.ToolboxApplication
import com.phongchen.toolbox.core.location.LocationProvider
import com.phongchen.toolbox.core.prefs.SettingsRepository
import com.phongchen.toolbox.core.sensor.SensorAvailability
import com.phongchen.toolbox.feature.flashlight.TorchController

/** Tiny manual dependency container, created once in the Application. */
val Context.container: AppContainer
    get() = (applicationContext as ToolboxApplication).container

class AppContainer(context: Context) {
    val sensorManager: SensorManager =
        context.getSystemService(SensorManager::class.java)
    val sensorAvailability: SensorAvailability = SensorAvailability(sensorManager)
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val locationProvider: LocationProvider = LocationProvider(context)
    val torchController: TorchController = TorchController(context)
}
