package com.phongchen.toolbox.feature.magnifier

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold
import com.phongchen.toolbox.core.ui.UnsupportedPanel
import com.phongchen.toolbox.core.ui.findActivity

@Composable
fun MagnifierScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = context.findActivity() as? LifecycleOwner
    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    // Keep the screen bright while magnifying.
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    // A holder keeps the pinch-gesture callback from capturing stale zoom state.
    val zoomHolder = remember { floatArrayOf(0f) }
    var sliderZoom by remember { mutableFloatStateOf(0f) }
    var torchOn by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<Bitmap?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraRef = remember { arrayOfNulls<Camera>(1) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    fun applyZoom(value: Float) {
        val z = value.coerceIn(0f, 1f)
        zoomHolder[0] = z
        sliderZoom = z
        cameraRef[0]?.cameraControl?.setLinearZoom(z)
    }

    if (granted && lifecycleOwner != null) {
        DisposableEffect(lensFacing) {
            val future = ProcessCameraProvider.getInstance(context)
            var provider: ProcessCameraProvider? = null
            future.addListener(
                {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    runCatching {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture,
                        )
                        cameraRef[0] = camera
                        flashAvailable = camera.cameraInfo.hasFlashUnit()
                        camera.cameraControl.setLinearZoom(zoomHolder[0])
                        camera.cameraControl.enableTorch(
                            torchOn && lensFacing == CameraSelector.LENS_FACING_BACK,
                        )
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            onDispose {
                provider?.unbindAll()
                cameraRef[0] = null
            }
        }
    }

    ToolScaffold(tool = Tool.MAGNIFIER, onBack = onBack) { padding ->
        when {
            !hasCamera -> UnsupportedPanel(
                message = stringResource(R.string.unsupported_camera),
                modifier = Modifier.padding(padding),
            )

            !granted -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ActionBanner(
                    message = stringResource(R.string.magnifier_permission_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }

            else -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(lensFacing) {
                            detectTransformGestures { _, _, zoom, _ ->
                                applyZoom(zoomHolder[0] + (zoom - 1f) * 0.15f)
                            }
                        },
                )

                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    Text(
                        text = stringResource(R.string.magnifier_mirror_mode),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .background(Color(0x88000000), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledIconButton(onClick = {
                        frozen = null
                        torchOn = false
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }) {
                        Icon(
                            Icons.Filled.Cameraswitch,
                            contentDescription = stringResource(R.string.magnifier_switch),
                        )
                    }
                    FilledIconButton(onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    frozen = image.toBitmap()
                                    image.close()
                                }
                            },
                        )
                    }) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(R.string.magnifier_freeze),
                        )
                    }
                    if (flashAvailable && lensFacing == CameraSelector.LENS_FACING_BACK) {
                        FilledIconButton(onClick = {
                            torchOn = !torchOn
                            cameraRef[0]?.cameraControl?.enableTorch(torchOn)
                        }) {
                            Icon(
                                if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = stringResource(R.string.magnifier_torch),
                            )
                        }
                    }
                    Slider(
                        value = sliderZoom,
                        onValueChange = ::applyZoom,
                        modifier = Modifier.weight(1f),
                    )
                }

                frozen?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                    )
                    IconButton(
                        onClick = { frozen = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.magnifier_unfreeze),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
