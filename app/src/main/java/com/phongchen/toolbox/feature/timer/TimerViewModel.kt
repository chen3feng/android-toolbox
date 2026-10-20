package com.phongchen.toolbox.feature.timer

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.viewModelScope
import com.phongchen.toolbox.core.ContainerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerMode { STOPWATCH, COUNTDOWN }

data class TimerUiState(
    val mode: TimerMode = TimerMode.STOPWATCH,
    val stopwatchRunning: Boolean = false,
    val elapsedMs: Long = 0L,
    val laps: List<Long> = emptyList(),
    val countdownRunning: Boolean = false,
    val totalMs: Long = 60_000L,
    val remainingMs: Long = 60_000L,
    val countdownFinished: Boolean = false,
)

class TimerViewModel(app: Application) : ContainerViewModel(app) {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState = _uiState.asStateFlow()

    private var ticker: Job? = null
    private var accumulatedMs = 0L
    private var anchorMs = 0L

    // Countdown bookkeeping.
    private var countdownRemainingMs = 60_000L
    private var countdownAnchorMs = 0L

    fun setMode(mode: TimerMode) {
        if (mode == _uiState.value.mode) return
        ticker?.cancel()
        accumulatedMs = 0L
        countdownRemainingMs = _uiState.value.totalMs
        _uiState.update {
            it.copy(
                mode = mode,
                stopwatchRunning = false,
                elapsedMs = 0L,
                laps = emptyList(),
                countdownRunning = false,
                remainingMs = it.totalMs,
                countdownFinished = false,
            )
        }
    }

    // ---- Stopwatch ----

    fun stopwatchStartPause() {
        val state = _uiState.value
        if (state.stopwatchRunning) {
            accumulatedMs = state.elapsedMs
            ticker?.cancel()
            _uiState.update { it.copy(stopwatchRunning = false) }
        } else {
            anchorMs = SystemClock.elapsedRealtime()
            ticker?.cancel()
            ticker = viewModelScope.launch {
                while (true) {
                    delay(TICK_MS)
                    _uiState.update {
                        it.copy(elapsedMs = accumulatedMs + (SystemClock.elapsedRealtime() - anchorMs))
                    }
                }
            }
            _uiState.update { it.copy(stopwatchRunning = true) }
        }
    }

    fun stopwatchLapOrReset() {
        val state = _uiState.value
        if (state.stopwatchRunning) {
            _uiState.update { it.copy(laps = it.laps + state.elapsedMs) }
        } else {
            ticker?.cancel()
            accumulatedMs = 0L
            _uiState.update {
                it.copy(stopwatchRunning = false, elapsedMs = 0L, laps = emptyList())
            }
        }
    }

    // ---- Countdown ----

    fun setCountdownMinutes(minutes: Int) {
        if (_uiState.value.countdownRunning) return
        val total = minutes.coerceIn(1, 99) * 60_000L
        countdownRemainingMs = total
        _uiState.update {
            it.copy(totalMs = total, remainingMs = total, countdownFinished = false)
        }
    }

    fun countdownStartPause() {
        val state = _uiState.value
        if (state.countdownRunning) {
            countdownRemainingMs = state.remainingMs
            ticker?.cancel()
            _uiState.update { it.copy(countdownRunning = false) }
        } else {
            countdownAnchorMs = SystemClock.elapsedRealtime()
            ticker?.cancel()
            ticker = viewModelScope.launch {
                while (true) {
                    delay(TICK_MS)
                    val remaining = countdownRemainingMs -
                        (SystemClock.elapsedRealtime() - countdownAnchorMs)
                    if (remaining <= 0L) {
                        _uiState.update {
                            it.copy(countdownRunning = false, remainingMs = 0L, countdownFinished = true)
                        }
                        playFinishSignal()
                        return@launch
                    }
                    _uiState.update { it.copy(remainingMs = remaining, countdownFinished = false) }
                }
            }
            _uiState.update { it.copy(countdownRunning = true, countdownFinished = false) }
        }
    }

    fun countdownReset() {
        ticker?.cancel()
        countdownRemainingMs = _uiState.value.totalMs
        _uiState.update {
            it.copy(
                countdownRunning = false,
                remainingMs = it.totalMs,
                countdownFinished = false,
            )
        }
    }

    /** Tone beeps block on sleeps; keep them off the main thread. */
    private suspend fun playFinishSignal() {
        withContext(Dispatchers.Default) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                repeat(3) {
                    tone.startTone(ToneGenerator.TONE_CDMA_PIP, 200)
                    Thread.sleep(260)
                }
                tone.release()
            }
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getApplication<Application>().getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(Vibrator::class.java)
            }
            vibrator?.takeIf { it.hasVibrator() }
                ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        }
    }

    private companion object {
        const val TICK_MS = 47L
    }
}
