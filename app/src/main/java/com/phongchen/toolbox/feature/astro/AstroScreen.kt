package com.phongchen.toolbox.feature.astro

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos

@Composable
fun AstroScreen(
    onBack: () -> Unit,
    viewModel: AstroViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    ToolScaffold(tool = Tool.ASTRO, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.hasLocationPermission) {
                ActionBanner(
                    message = stringResource(R.string.astro_permission_rationale),
                    actionLabel = stringResource(R.string.common_grant),
                    onAction = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                )
            } else if (state.waitingForFix || state.apparentSolarSeconds == null) {
                Text(
                    text = stringResource(R.string.gps_waiting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SolarTimeCard(state)
                SunCard(state)
                MoonCard(state)
                Text(
                    text = stringResource(R.string.astro_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SolarTimeCard(state: AstroUiState) {
    SectionCard(title = stringResource(R.string.astro_solar_time)) {
        Text(
            text = AstroEngine.formatClock(state.apparentSolarSeconds!!),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        TimeRow(
            label = stringResource(R.string.astro_mean_solar_time),
            value = AstroEngine.formatClock(state.meanSolarSeconds!!),
        )
        state.equationOfTimeSeconds?.let { eot ->
            TimeRow(
                label = stringResource(R.string.astro_equation_of_time),
                value = AstroEngine.formatSignedDuration(eot),
            )
        }
    }
}

@Composable
private fun SunCard(state: AstroUiState) {
    val times = state.sunTimes
    SectionCard(title = stringResource(R.string.astro_sun)) {
        if (times != null && state.latitude != null) {
            val noonAlt = AstroEngine.solarNoonAltitudeDeg(state.latitude, times.transitJd)
            TimeRow(
                label = stringResource(R.string.astro_noon_altitude),
                value = "%.1f°".format(noonAlt),
            )
        }
        when {
            times == null -> Unit
            times.sunNeverSets -> Text(
                stringResource(R.string.astro_polar_day),
                style = MaterialTheme.typography.bodyLarge,
            )
            times.sunNeverRises -> Text(
                stringResource(R.string.astro_polar_night),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SunColumn(
                        label = stringResource(R.string.astro_sunrise),
                        jd = times.sunriseJd,
                    )
                    SunColumn(
                        label = stringResource(R.string.astro_sunset),
                        jd = times.sunsetJd,
                    )
                }
                Spacer(Modifier.height(8.dp))
                AstroEngine.daylightSeconds(times.sunriseJd, times.sunsetJd)?.let { sec ->
                    TimeRow(
                        label = stringResource(R.string.astro_daylight),
                        value = formatDayLength(sec),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoonCard(state: AstroUiState) {
    val moon = state.moon ?: return
    val phaseNames = stringArrayResource(R.array.astro_moon_phases)
    SectionCard(title = stringResource(R.string.astro_moon_phase)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoonDisc(
                elongationDeg = moon.elongationDeg,
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.size(20.dp))
            Column {
                Text(
                    text = phaseNames[moon.phaseIndex],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.astro_illumination,
                        (moon.illumination * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SunColumn(label: String, jd: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = jd?.let {
                val zdt = Instant.ofEpochMilli(AstroEngine.epochMillisFromJulianDay(it))
                    .atZone(ZoneId.systemDefault())
                "%02d:%02d".format(zdt.hour, zdt.minute)
            } ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
        }
    }
}

private fun formatDayLength(seconds: Double): String {
    val total = seconds.toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    return "%d:%02d".format(h, m)
}

private val MoonDark = Color(0xFF2A2D33)
private val MoonLit = Color(0xFFF3EBC9)

/**
 * Draws the lunar disc for an elongation angle [elongationDeg] (0 = new, 90 = first
 * quarter, 180 = full, 270 = last quarter), as seen from the northern hemisphere
 * (waxing limb on the right).
 */
@Composable
private fun MoonDisc(elongationDeg: Double, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val center = Offset(r, r)
        val full = Rect(0f, 0f, 2f * r, 2f * r)

        // Dark limb background.
        drawCircle(color = MoonDark, radius = r, center = center)

        val theta = Math.toRadians(elongationDeg)
        val waxing = elongationDeg < 180.0
        val gibbous = cos(theta) < 0.0

        // Near full / new the terminator degenerates into a diameter; fill all or none.
        if (absCos(theta) > 0.999f) {
            if (gibbous) drawCircle(color = MoonLit, radius = r, center = center)
        } else {
            // Terminator is the half of an ellipse with horizontal semi-axis r|cos E|.
            // Build the lit region as ONE closed path (outer limb + terminator) so no
            // seam appears where two fills meet.
            val rx = r * absCos(theta)
            val term = Rect(center.x - rx, center.y - r, center.x + rx, center.y + r)
            val outerSweep = if (waxing) 180f else -180f
            val terminatorSweep = if (waxing == gibbous) 180f else -180f
            val path = Path().apply {
                moveTo(center.x, 0f)
                arcTo(full, -90f, outerSweep, forceMoveTo = false)
                arcTo(term, 90f, terminatorSweep, forceMoveTo = false)
                close()
            }
            drawPath(path, color = MoonLit)
        }

        // Thin outline for the dark limb on light backgrounds.
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = r,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
        )
    }
}

private fun absCos(rad: Double): Float = kotlin.math.abs(cos(rad)).toFloat()
