package com.phongchen.toolbox.feature.audio

import android.app.Application
import com.phongchen.toolbox.core.ContainerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AudioToolMode { METRONOME, TONE }

data class AudioUiState(
    val mode: AudioToolMode = AudioToolMode.METRONOME,
    val playing: Boolean = false,
    val bpm: Int = 100,
    val beatsPerMeasure: Int = 4,
    val currentBeat: Int = -1,
    val frequencyHz: Double = 440.0,
    val sliderPosition: Float = AudioMath.frequencyToSlider(440.0),
    val waveform: WaveformType = WaveformType.SINE,
    val volume: Float = 0.5f,
)

class AudioViewModel(app: Application) : ContainerViewModel(app) {

    private val engine = AudioEngine().apply {
        onBeat = { beat -> _uiState.update { it.copy(currentBeat = beat) } }
    }
    private val tapTimestamps = ArrayDeque<Long>()

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState = _uiState.asStateFlow()

    fun setMode(mode: AudioToolMode) {
        if (mode == _uiState.value.mode) return
        engine.stop()
        tapTimestamps.clear()
        _uiState.update {
            it.copy(mode = mode, playing = false, currentBeat = -1)
        }
    }

    fun togglePlay() {
        val state = _uiState.value
        if (state.playing) {
            engine.stop()
            _uiState.update { it.copy(playing = false, currentBeat = -1) }
        } else {
            when (state.mode) {
                AudioToolMode.METRONOME -> {
                    engine.bpm = state.bpm
                    engine.beatsPerMeasure = state.beatsPerMeasure
                    engine.volume = state.volume
                    engine.startMetronome()
                }
                AudioToolMode.TONE -> {
                    engine.frequencyHz = state.frequencyHz
                    engine.waveform = state.waveform
                    engine.volume = state.volume
                    engine.startTone()
                }
            }
            _uiState.update { it.copy(playing = true) }
        }
    }

    fun setBpm(bpm: Int) {
        val value = bpm.coerceIn(MIN_BPM, MAX_BPM)
        engine.bpm = value
        _uiState.update { it.copy(bpm = value) }
    }

    fun setBeats(beats: Int) {
        val value = beats.coerceIn(MIN_BEATS, MAX_BEATS)
        engine.beatsPerMeasure = value
        _uiState.update { it.copy(beatsPerMeasure = value, currentBeat = -1) }
    }

    /** Tap tempo: averages the gaps between the last four taps. */
    fun tapTempo() {
        val now = System.currentTimeMillis()
        if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > TAP_RESET_MS) {
            tapTimestamps.clear()
        }
        tapTimestamps.addLast(now)
        while (tapTimestamps.size > 5) tapTimestamps.removeFirst()
        if (tapTimestamps.size >= 2) {
            val span = now - tapTimestamps.first()
            val gaps = tapTimestamps.size - 1
            setBpm((60_000L * gaps / span).toInt())
        }
    }

    fun setFrequencyBySlider(position: Float) {
        val freq = AudioMath.sliderToFrequency(position.coerceIn(0f, 1f))
        engine.frequencyHz = freq
        _uiState.update { it.copy(sliderPosition = position, frequencyHz = freq) }
    }

    fun setFrequency(freq: Double) {
        engine.frequencyHz = freq
        _uiState.update {
            it.copy(frequencyHz = freq, sliderPosition = AudioMath.frequencyToSlider(freq))
        }
    }

    fun setWaveform(type: WaveformType) {
        engine.waveform = type
        _uiState.update { it.copy(waveform = type) }
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        engine.volume = v
        _uiState.update { it.copy(volume = v) }
    }

    /** Leaving the screen: never leave sound playing. */
    fun onScreenStopped() {
        engine.stop()
        _uiState.update { it.copy(playing = false, currentBeat = -1) }
    }

    override fun onCleared() {
        engine.stop()
        super.onCleared()
    }

    private companion object {
        const val MIN_BPM = 40
        const val MAX_BPM = 240
        const val MIN_BEATS = 2
        const val MAX_BEATS = 7
        const val TAP_RESET_MS = 2_000L
    }
}
