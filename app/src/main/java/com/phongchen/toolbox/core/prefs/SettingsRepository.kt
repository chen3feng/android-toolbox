package com.phongchen.toolbox.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "toolbox_settings")

data class ToolSettings(
    val trueNorthEnabled: Boolean,
    val metricUnits: Boolean,
    val qnhHpa: Double,
    val levelZeroPitch: Float,
    val levelZeroRoll: Float,
    val soundGainDb: Float,
    val toolOrder: List<String>,
) {
    val calibrated: Boolean get() = qnhHpa != STANDARD_QNH_HPA

    companion object {
        const val STANDARD_QNH_HPA: Double = 1013.25

        /** Default dBFS→SPL mapping; mirrors SoundMeterEngine.DEFAULT_GAIN_DB. */
        const val DEFAULT_SOUND_GAIN_DB: Float = 90f

        val DEFAULT = ToolSettings(
            trueNorthEnabled = true,
            metricUnits = true,
            qnhHpa = STANDARD_QNH_HPA,
            levelZeroPitch = 0f,
            levelZeroRoll = 0f,
            soundGainDb = DEFAULT_SOUND_GAIN_DB,
            toolOrder = emptyList(),
        )
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val TRUE_NORTH = booleanPreferencesKey("true_north")
        val METRIC = booleanPreferencesKey("metric_units")
        val QNH = doublePreferencesKey("qnh_hpa")
        val LEVEL_ZERO_PITCH = floatPreferencesKey("level_zero_pitch")
        val LEVEL_ZERO_ROLL = floatPreferencesKey("level_zero_roll")
        val SOUND_GAIN_DB = floatPreferencesKey("sound_gain_db")
        val TOOL_ORDER = stringPreferencesKey("tool_order")
    }

    val settings: Flow<ToolSettings> = context.dataStore.data.map { prefs ->
        ToolSettings(
            trueNorthEnabled = prefs[Keys.TRUE_NORTH] ?: true,
            metricUnits = prefs[Keys.METRIC] ?: true,
            qnhHpa = prefs[Keys.QNH] ?: ToolSettings.STANDARD_QNH_HPA,
            levelZeroPitch = prefs[Keys.LEVEL_ZERO_PITCH] ?: 0f,
            levelZeroRoll = prefs[Keys.LEVEL_ZERO_ROLL] ?: 0f,
            soundGainDb = prefs[Keys.SOUND_GAIN_DB] ?: ToolSettings.DEFAULT_SOUND_GAIN_DB,
            toolOrder = prefs[Keys.TOOL_ORDER]
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
    }

    suspend fun setTrueNorth(enabled: Boolean) = edit { it[Keys.TRUE_NORTH] = enabled }

    suspend fun setMetricUnits(metric: Boolean) = edit { it[Keys.METRIC] = metric }

    suspend fun setQnh(hpa: Double) = edit { it[Keys.QNH] = hpa }

    suspend fun resetQnh() = edit { it[Keys.QNH] = ToolSettings.STANDARD_QNH_HPA }

    suspend fun setLevelZero(pitch: Float, roll: Float) = edit {
        it[Keys.LEVEL_ZERO_PITCH] = pitch
        it[Keys.LEVEL_ZERO_ROLL] = roll
    }

    suspend fun resetLevelZero() = edit {
        it[Keys.LEVEL_ZERO_PITCH] = 0f
        it[Keys.LEVEL_ZERO_ROLL] = 0f
    }

    suspend fun setSoundGain(db: Float) = edit { it[Keys.SOUND_GAIN_DB] = db }

    suspend fun resetSoundGain() = edit {
        it[Keys.SOUND_GAIN_DB] = ToolSettings.DEFAULT_SOUND_GAIN_DB
    }

    suspend fun setToolOrder(routes: List<String>) = edit {
        it[Keys.TOOL_ORDER] = routes.joinToString(",")
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
