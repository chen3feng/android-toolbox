package com.phongchen.toolbox.feature.gps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phongchen.toolbox.R
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.core.ui.ActionBanner
import com.phongchen.toolbox.core.ui.ToolScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GpsScreen(
    onBack: () -> Unit,
    viewModel: GpsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }
    ToolScaffold(tool = Tool.GPS, onBack = onBack) { padding ->
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
                    message = stringResource(R.string.gps_permission_rationale),
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
            } else if (state.waitingForFix) {
                Text(
                    text = stringResource(R.string.gps_waiting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val lat = state.latitude
            val lon = state.longitude
            if (lat != null && lon != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${GpsEngine.decimalDegrees(lat)}  ${GpsEngine.decimalDegrees(lon)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${GpsEngine.dms(lat, latitude = true)}  ${GpsEngine.dms(lon, latitude = false)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                clipboard.setText(
                                    AnnotatedString(GpsEngine.shareText(lat, lon, state.altitudeM)),
                                )
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                Text(
                                    stringResource(R.string.gps_copy),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        GpsEngine.shareText(lat, lon, state.altitudeM),
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, null),
                                )
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Text(
                                    stringResource(R.string.gps_share),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            OutlinedButton(onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(GpsEngine.geoUri(lat, lon)),
                                        ),
                                    )
                                }.onFailure { if (it is ActivityNotFoundException) Unit }
                            }) {
                                Icon(Icons.Filled.Map, contentDescription = null)
                                Text(
                                    stringResource(R.string.gps_open_map),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }

                InfoCard(
                    stringResource(R.string.gps_altitude),
                    state.altitudeM?.let { alt ->
                        val vAcc = state.verticalAccuracyM
                        if (state.metricUnits) {
                            "%.1f m%s".format(alt, vAcc?.let { " (±%.0f m)".format(it) } ?: "")
                        } else {
                            "%.1f ft%s".format(
                                alt * 3.28084,
                                vAcc?.let { " (±%.0f ft)".format(it * 3.28084f) } ?: "",
                            )
                        }
                    } ?: "—",
                )
                InfoCard(
                    stringResource(R.string.gps_accuracy),
                    state.accuracyM?.let { acc ->
                        if (state.metricUnits) "±%.1f m".format(acc) else "±%.1f ft".format(acc * 3.28084)
                    } ?: "—",
                )
                val speed = state.speedMps
                val bearing = GpsEngine.bearingLabel(state.bearing ?: Float.NaN)
                InfoCard(
                    stringResource(R.string.gps_speed_heading),
                    if (speed != null) {
                        val speedText = if (state.metricUnits) {
                            "%.0f km/h".format(speed * 3.6f)
                        } else {
                            "%.0f mph".format(speed * 2.236936f)
                        }
                        if (bearing != null) "$speedText  ·  $bearing" else speedText
                    } else {
                        "—"
                    },
                )
                state.satellites?.let { sats ->
                    InfoCard(
                        stringResource(R.string.gps_satellites),
                        "%d / %d".format(state.satellitesUsed ?: 0, sats),
                        Icons.Filled.SatelliteAlt,
                    )
                }
                InfoCard(
                    stringResource(R.string.gps_source_time),
                    buildString {
                        append(state.provider ?: "—")
                        state.fixTimeMs?.let {
                            append("  ·  ")
                            append(timeFormatter.format(Date(it)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
