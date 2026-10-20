package com.phongchen.toolbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ToolboxTheme
import com.phongchen.toolbox.feature.altimeter.AltimeterScreen
import com.phongchen.toolbox.feature.astro.AstroScreen
import com.phongchen.toolbox.feature.audio.AudioScreen
import com.phongchen.toolbox.feature.compass.CompassScreen
import com.phongchen.toolbox.feature.flashlight.FlashlightScreen
import com.phongchen.toolbox.feature.gps.GpsScreen
import com.phongchen.toolbox.feature.home.HomeScreen
import com.phongchen.toolbox.feature.level.LevelScreen
import com.phongchen.toolbox.feature.light.LightScreen
import com.phongchen.toolbox.feature.magnifier.MagnifierScreen
import com.phongchen.toolbox.feature.measure.MeasureScreen
import com.phongchen.toolbox.feature.metal.MetalScreen
import com.phongchen.toolbox.feature.sound.SoundScreen
import com.phongchen.toolbox.feature.speed.SpeedScreen
import com.phongchen.toolbox.feature.step.StepScreen
import com.phongchen.toolbox.feature.timer.TimerScreen
import com.phongchen.toolbox.feature.vibration.VibrationScreen
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // Home always sits under the deep-linked tool so Back behaves naturally.
    private val deepLinkRoutes = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkRoutes.value = routeFrom(intent)
        setContent {
            ToolboxTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    deepLinkRoutes.collect { route ->
                        if (route != null) {
                            navController.navigate(route) { launchSingleTop = true }
                            deepLinkRoutes.value = null
                        }
                    }
                }
                NavHost(navController = navController, startDestination = ROUTE_HOME) {
                    composable(ROUTE_HOME) {
                        HomeScreen(onOpenTool = { tool -> navController.navigate(tool.route) })
                    }
                    composable(Tool.COMPASS.route) {
                        CompassScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.LIGHT.route) {
                        LightScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.ALTIMETER.route) {
                        AltimeterScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.FLASHLIGHT.route) {
                        FlashlightScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.LEVEL.route) {
                        LevelScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.SOUND.route) {
                        SoundScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.SPEED.route) {
                        SpeedScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.METAL.route) {
                        MetalScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.STEP.route) {
                        StepScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.GPS.route) {
                        GpsScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.VIBRATION.route) {
                        VibrationScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.AUDIO.route) {
                        AudioScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.TIMER.route) {
                        TimerScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.MEASURE.route) {
                        MeasureScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.MAGNIFIER.route) {
                        MagnifierScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Tool.ASTRO.route) {
                        AstroScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFrom(intent)?.let { deepLinkRoutes.value = it }
    }

    private fun routeFrom(intent: Intent?): String? =
        Tool.fromRoute(intent?.getStringExtra(EXTRA_TOOL_ROUTE))?.route

    companion object {
        const val EXTRA_TOOL_ROUTE = "com.phongchen.toolbox.extra.TOOL_ROUTE"
        const val ROUTE_HOME = "home"
    }
}
