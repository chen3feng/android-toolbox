package com.phongchen.toolbox.feature.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import com.phongchen.toolbox.core.Tool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val tools: List<Tool> = Tool.entries,
)

class HomeViewModel(app: Application) : ContainerViewModel(app) {

    val uiState = container.settingsRepository.settings
        .map { HomeUiState(tools = Tool.orderedBy(it.toolOrder)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    fun saveOrder(tools: List<Tool>) {
        viewModelScope.launch {
            container.settingsRepository.setToolOrder(tools.map { it.route })
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
