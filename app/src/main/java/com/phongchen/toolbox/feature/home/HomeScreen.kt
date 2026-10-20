package com.phongchen.toolbox.feature.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.container
import com.phongchen.toolbox.core.ui.findActivity
import com.phongchen.toolbox.feature.flashlight.TorchState
import com.phongchen.toolbox.widget.WidgetPinner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTool: (Tool) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pinUnsupported = stringResource(R.string.pin_unsupported_snackbar)
    val pinRequested = stringResource(R.string.pin_success)
    val availability = context.container.sensorAvailability
    val torchState by context.container.torchController.state.collectAsState()
    val vmState by homeViewModel.uiState.collectAsState()

    fun pinTool(tool: Tool) {
        val activity = context.findActivity()
        val handled = activity != null &&
            WidgetPinner.isSupported(activity) &&
            WidgetPinner.requestPin(activity, tool)
        scope.launch {
            snackbarHostState.showSnackbar(if (handled) pinRequested else pinUnsupported)
        }
    }

    // Local working copy while dragging; resyncs from the persisted order otherwise.
    var tools by remember { mutableStateOf(vmState.tools) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(vmState.tools) {
        if (draggingIndex == -1) tools = vmState.tools
    }

    // Slot rectangles in the grid wrapper's coordinate space.
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    // Finger position in the same coordinate space; the dragged card is pinned to it.
    var dragFinger by remember { mutableStateOf(Offset.Unspecified) }
    val haptic = LocalHapticFeedback.current

    fun isSupported(tool: Tool) = when (tool) {
        Tool.COMPASS -> availability.hasCompass
        Tool.LIGHT -> availability.hasLight
        Tool.ALTIMETER -> true // GPS-only devices still work; pressure is optional
        Tool.FLASHLIGHT -> torchState !is TorchState.Unavailable
        Tool.LEVEL -> availability.hasLevel
        Tool.SOUND ->
            context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_MICROPHONE,
            )
        Tool.SPEED -> true // GPS is optional on the hardware level
        Tool.METAL -> availability.hasMagnetometer
        Tool.STEP -> availability.hasStepCounter
        Tool.GPS -> true
        Tool.VIBRATION -> availability.hasVibration
        Tool.AUDIO -> true // speakers are always present
        Tool.TIMER -> true
        Tool.MEASURE -> availability.hasLevel
        Tool.MAGNIFIER ->
            context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_CAMERA_ANY,
            )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            slotRects.entries.firstOrNull { it.value.contains(start) }
                                ?.key
                                ?.let {
                                    draggingIndex = it
                                    dragFinger = start
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                        },
                        onDragEnd = {
                            if (draggingIndex != -1) {
                                draggingIndex = -1
                                dragFinger = Offset.Unspecified
                                homeViewModel.saveOrder(tools)
                            }
                        },
                        onDragCancel = {
                            draggingIndex = -1
                            dragFinger = Offset.Unspecified
                        },
                        onDrag = { change, delta ->
                            val current = draggingIndex
                            if (current == -1) return@detectDragGesturesAfterLongPress
                            change.consume()
                            dragFinger += delta
                            val finger = dragFinger
                            val target = slotRects.entries
                                .firstOrNull { it.key != current && it.value.contains(finger) }
                                ?.key
                                ?: return@detectDragGesturesAfterLongPress
                            // Reorder only; the dragged card's translation is derived from the
                            // finger position each frame, so it stays pinned through the swap.
                            tools = tools.toMutableList().apply { add(target, removeAt(current)) }
                            draggingIndex = target
                        },
                    )
                },
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { gridOrigin = it.positionInRoot() },
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(tools, key = { _, tool -> tool.route }) { index, tool ->
                    val isDragging = draggingIndex == index
                    ToolCard(
                        tool = tool,
                        supported = isSupported(tool),
                        dragging = isDragging,
                        onClick = { if (draggingIndex == -1) onOpenTool(tool) },
                        onPin = { pinTool(tool) },
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                slotRects[index] = Rect(
                                    offset = coords.positionInRoot() - gridOrigin,
                                    size = androidx.compose.ui.geometry.Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat(),
                                    ),
                                )
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                val finger = dragFinger
                                val center = slotRects[index]?.center
                                if (isDragging && finger != Offset.Unspecified && center != null) {
                                    translationX = finger.x - center.x
                                    translationY = finger.y - center.y
                                    scaleX = DRAG_SCALE
                                    scaleY = DRAG_SCALE
                                }
                            },
                    )
                }
            }
        }
    }
}

private const val DRAG_SCALE = 1.06f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolCard(
    tool: Tool,
    supported: Boolean,
    dragging: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(width = 168.dp, height = 132.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (supported) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (dragging) 12.dp else 0.dp,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(
                    painter = painterResource(tool.iconRes),
                    contentDescription = null,
                    tint = if (supported) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(tool.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = if (supported) {
                        stringResource(tool.subtitleRes)
                    } else {
                        stringResource(R.string.common_unsupported)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Shortcut to pin this tool's home-screen widget without opening the tool first.
            if (supported) {
                IconButton(
                    onClick = onPin,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.AddHome,
                        contentDescription = stringResource(R.string.common_add_to_home),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
