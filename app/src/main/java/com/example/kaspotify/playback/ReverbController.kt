package com.example.kaspotify.playback

import android.media.audiofx.PresetReverb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Reverb presets exposed to the UI, mapped to [PresetReverb] preset constants. */
enum class ReverbPreset(val label: String, val presetValue: Short) {
    NONE("None", PresetReverb.PRESET_NONE),
    SMALL_ROOM("Small room", PresetReverb.PRESET_SMALLROOM),
    MEDIUM_ROOM("Medium room", PresetReverb.PRESET_MEDIUMROOM),
    LARGE_ROOM("Large room", PresetReverb.PRESET_LARGEROOM),
    MEDIUM_HALL("Medium hall", PresetReverb.PRESET_MEDIUMHALL),
    LARGE_HALL("Large hall", PresetReverb.PRESET_LARGEHALL),
    PLATE("Plate", PresetReverb.PRESET_PLATE)
}

/**
 * Owns a [PresetReverb] bound to the ExoPlayer audio session, same attach/release pattern as
 * [EqualizerController]. Used by the "Slow + Reverb" effects mode.
 */
@Singleton
class ReverbController @Inject constructor() {

    private var reverb: PresetReverb? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _preset = MutableStateFlow(ReverbPreset.NONE)
    val preset: StateFlow<ReverbPreset> = _preset.asStateFlow()

    /** Called from the service when the audio session id is known. Safe to call repeatedly. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            val effect = PresetReverb(EFFECT_PRIORITY, audioSessionId)
            reverb = effect
            _available.value = true
        } catch (t: Throwable) {
            reverb = null
            _available.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        val effect = reverb ?: return
        try {
            effect.enabled = enabled
            _enabled.value = effect.enabled
        } catch (_: Throwable) {
        }
    }

    fun setPreset(preset: ReverbPreset) {
        val effect = reverb ?: return
        try {
            effect.preset = preset.presetValue
            _preset.value = preset
        } catch (_: Throwable) {
        }
    }

    fun release() {
        reverb?.runCatching {
            enabled = false
            release()
        }
        reverb = null
        _available.value = false
        _enabled.value = false
        _preset.value = ReverbPreset.NONE
    }

    companion object {
        private const val EFFECT_PRIORITY = 0
    }
}
