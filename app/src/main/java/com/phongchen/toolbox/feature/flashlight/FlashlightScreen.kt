package com.phongchen.toolbox.feature.flashlight

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.container
import com.phongchen.toolbox.core.ui.findActivity
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold

@Composable
fun FlashlightScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = context.container.torchController
    val torchState by controller.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    // Survives auto-rotate Activity recreation so the white screen doesn't turn off.
    var whiteScreen by rememberSaveable { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    val busyText = stringResource(R.string.flashlight_busy)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (granted) busyMessage = null
    }

    // Keep the display on while the flashlight screen is open.
    val activity = context.findActivity()
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Full-brightness white screen fallback.
    DisposableEffect(whiteScreen) {
        val window = activity?.window
        val layoutParams = window?.attributes
        val previousBrightness = layoutParams?.screenBrightness
        if (whiteScreen && window != null && layoutParams != null) {
            window.attributes = layoutParams.apply { screenBrightness = 1f }
        }
        onDispose {
            if (window != null && layoutParams != null) {
                window.attributes = layoutParams.apply { screenBrightness = previousBrightness ?: -1f }
            }
        }
    }

    if (whiteScreen) {
        WhiteScreen(onExit = { whiteScreen = false })
        return
    }

    ToolScaffold(tool = Tool.FLASHLIGHT, onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                torchState is TorchState.Unavailable -> {
                    Text(
                        text = stringResource(R.string.unsupported_flash),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(32.dp),
                    )
                    Button(onClick = { whiteScreen = true }) {
                        Text(stringResource(R.string.flashlight_screen_mode))
                    }
                }

                !hasCameraPermission -> ActionBanner(
                    message = stringResource(R.string.flashlight_camera_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )

                else -> {
                    val on = torchState is TorchState.On
                    FilledIconButton(
                        onClick = {
                            val accepted = controller.setTorch(!on)
                            busyMessage = if (accepted) null else busyText
                        },
                        modifier = Modifier.size(180.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (on) Color(0xFFFFC107) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (on) Color(0xFF5D4037) else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tool_flashlight),
                            contentDescription = stringResource(
                                if (on) R.string.flashlight_turn_off else R.string.flashlight_turn_on,
                            ),
                            modifier = Modifier.size(88.dp),
                        )
                    }
                    Text(
                        text = stringResource(
                            if (on) R.string.flashlight_on else R.string.flashlight_off,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    busyMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { whiteScreen = true },
                        modifier = Modifier.padding(top = 32.dp),
                    ) {
                        Text(stringResource(R.string.flashlight_screen_mode))
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteScreen(onExit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.BottomCenter,
    ) {
        FilledIconButton(
            onClick = onExit,
            modifier = Modifier.padding(bottom = 48.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.flashlight_turn_off))
        }
    }
}
