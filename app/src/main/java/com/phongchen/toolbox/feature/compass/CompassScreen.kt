package com.phongchen.toolbox.feature.compass

import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold
import kotlin.math.cos
import kotlin.math.sin

private const val TAB_MAGNETIC = 0
private const val TAB_SUN = 1

@Composable
fun CompassScreen(
    onBack: () -> Unit,
    viewModel: CompassViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(TAB_MAGNETIC) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }
    val requestLocation = {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    ToolScaffold(tool = Tool.COMPASS, onBack = onBack) { padding ->
        if (!state.supported) {
            com.phongchen.toolbox.core.ui.UnsupportedPanel(
                message = stringResource(R.string.unsupported_compass),
                modifier = Modifier.padding(padding),
            )
            return@ToolScaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_MAGNETIC,
                    onClick = { selectedTab = TAB_MAGNETIC },
                    text = { Text(stringResource(R.string.compass_tab_magnetic)) },
                )
                Tab(
                    selected = selectedTab == TAB_SUN,
                    onClick = { selectedTab = TAB_SUN },
                    text = { Text(stringResource(R.string.compass_tab_celestial)) },
                )
            }
            when (selectedTab) {
                TAB_MAGNETIC -> MagneticTab(state, viewModel, requestLocation)
                TAB_SUN -> CelestialTab(state, viewModel, requestLocation)
            }
        }
    }
}

@Composable
private fun MagneticTab(
    state: CompassUiState,
    viewModel: CompassViewModel,
    requestLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (state.accuracy in 0..SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            ActionBanner(
                message = stringResource(R.string.compass_low_accuracy),
                actionLabel = stringResource(R.string.common_ok),
                onAction = { },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        CompassDial(
            azimuth = state.azimuthDeg,
            labels = listOf(
                stringResource(R.string.compass_north),
                stringResource(R.string.compass_east),
                stringResource(R.string.compass_south),
                stringResource(R.string.compass_west),
            ),
            modifier = Modifier.size(300.dp).padding(16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "%3d°".format(state.azimuthDeg.toInt()),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = CompassEngine.directionLabel(state.azimuthDeg),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        FilterChip(
            selected = state.trueNorthEnabled,
            onClick = { viewModel.setTrueNorth(!state.trueNorthEnabled) },
            label = {
                Text(
                    if (state.trueNorthEnabled) {
                        stringResource(R.string.compass_true)
                    } else {
                        stringResource(R.string.compass_magnetic)
                    },
                )
            },
        )
        state.declinationDeg?.let { decl ->
            Text(
                text = stringResource(R.string.compass_declination, decl),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (state.trueNorthEnabled && !state.hasLocationPermission) {
            ActionBanner(
                message = stringResource(R.string.compass_location_rationale),
                actionLabel = stringResource(R.string.common_grant),
                onAction = requestLocation,
            )
        }
    }
}

@Composable
private fun CelestialTab(
    state: CompassUiState,
    viewModel: CompassViewModel,
    requestLocation: () -> Unit,
) {
    // Collect gyro + compute body positions only while this tab is on screen.
    DisposableEffect(Unit) {
        viewModel.setCelestialMode(true)
        onDispose { viewModel.setCelestialMode(false) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(12.dp))
        if (!state.hasGameRotation) {
            com.phongchen.toolbox.core.ui.UnsupportedPanel(
                message = stringResource(R.string.compass_sun_no_gyro),
            )
            return@Column
        }
        if (!state.hasLocationPermission) {
            ActionBanner(
                message = stringResource(R.string.compass_location_rationale),
                actionLabel = stringResource(R.string.common_grant),
                onAction = requestLocation,
            )
            return@Column
        }
        val sunAz = state.sunAzimuthDeg
        val moonAz = state.moonAzimuthDeg
        if (sunAz == null || moonAz == null) {
            Text(
                text = stringResource(R.string.gps_waiting),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val heading = state.celestialHeadingDeg
        if (heading != null) {
            CompassDial(
                azimuth = heading,
                labels = listOf(
                    stringResource(R.string.compass_north),
                    stringResource(R.string.compass_east),
                    stringResource(R.string.compass_south),
                    stringResource(R.string.compass_west),
                ),
                modifier = Modifier.size(220.dp).padding(10.dp),
            )
            Text(
                text = "%3d°".format(heading.toInt()),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = CompassEngine.directionLabel(heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }

        val usingMoon = state.referenceBody == CompassViewModel.BODY_MOON
        val referenceVisible = state.referenceBody != null
        val bodyName = stringResource(
            if (usingMoon) R.string.compass_body_moon else R.string.compass_body_sun,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyRow(
                    label = stringResource(R.string.compass_sun_azimuth),
                    value = "%.1f° / %.1f°".format(sunAz, state.sunAltitudeDeg ?: 0.0),
                    active = state.referenceBody == CompassViewModel.BODY_SUN,
                )
                BodyRow(
                    label = stringResource(R.string.compass_moon_azimuth),
                    value = "%.1f° / %.1f°".format(moonAz, state.moonAltitudeDeg ?: 0.0),
                    active = usingMoon,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.calibrateCelestialHeading() },
            enabled = referenceVisible,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.compass_celestial_calibrate, bodyName))
        }
        if (state.celestialCalibrated) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.compass_celestial_calibrated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { viewModel.clearCelestialCalibration() }) {
                    Text(stringResource(R.string.compass_sun_recalibrate))
                }
            }
        }
        if (!referenceVisible) {
            Text(
                text = stringResource(R.string.compass_both_below),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.compass_sun_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BodyRow(label: String, value: String, active: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label + if (active) "  ●" else "",
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun CompassDial(
    azimuth: Float,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val northColor = Color(0xFFD32F2F)

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f

        val cardinalPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 42f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Rotate the dial opposite to the heading so the faced direction sits at the top.
        rotate(degrees = -azimuth, pivot = center) {
            for (i in 0 until 72) {
                val angle = i * 5f
                val major = i % 9 == 0 // 0/45/90/...
                val tickInner = outer - if (major) 28f else 16f
                val rad = Math.toRadians(angle.toDouble()) - Math.PI / 2
                val sx = center.x + (tickInner * cos(rad)).toFloat()
                val sy = center.y + (tickInner * sin(rad)).toFloat()
                val ex = center.x + (outer * cos(rad)).toFloat()
                val ey = center.y + (outer * sin(rad)).toFloat()
                drawLine(
                    color = if (major) onSurface else onSurfaceVariant,
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = if (major) 3f else 1.5f,
                )
            }

            val labelRadius = outer - 54f
            labels.forEachIndexed { index, label ->
                val angle = index * 90f
                val rad = Math.toRadians(angle.toDouble()) - Math.PI / 2
                val x = center.x + (labelRadius * cos(rad)).toFloat()
                val y = center.y + (labelRadius * sin(rad)).toFloat()
                cardinalPaint.color =
                    if (index == 0) northColor.toArgb() else onSurface.toArgb()
                // Counter-rotate the label so letters stay upright.
                val nativeCanvas = drawContext.canvas.nativeCanvas
                drawContext.canvas.withSave {
                    nativeCanvas.translate(x, y)
                    nativeCanvas.rotate(azimuth)
                    nativeCanvas.drawText(label, 0f, cardinalPaint.textSize / 3f, cardinalPaint)
                }
            }
        }

        // Fixed top pointer.
        drawCircle(color = primary.copy(alpha = 0.12f), radius = outer - 80f, center = center)
        val tipY = center.y - outer + 18f
        drawLine(
            color = northColor,
            start = Offset(center.x, tipY),
            end = Offset(center.x, tipY + 34f),
            strokeWidth = 8f,
        )
        drawCircle(color = northColor, radius = 8f, center = Offset(center.x, tipY))
    }
}
