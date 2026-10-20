package com.phongchen.toolbox.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.phongchen.toolbox.ToolboxApplication

abstract class ContainerViewModel(app: Application) : AndroidViewModel(app) {
    protected val container: AppContainer
        get() = getApplication<ToolboxApplication>().container
}
