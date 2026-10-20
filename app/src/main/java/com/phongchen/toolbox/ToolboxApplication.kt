package com.phongchen.toolbox

import android.app.Application
import com.phongchen.toolbox.core.AppContainer

class ToolboxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
