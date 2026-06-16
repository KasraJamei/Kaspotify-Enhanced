package com.example.kaspotify.playback

import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Volume normalization via [LoudnessEnhancer] bound to the ExoPlayer audio session. When enabled it
 * applies a constant make-up gain so quieter tracks come up toward a consistent perceived loudness
 * (a lightweight, always-available alternative to per-file ReplayGain analysis). Same attach/release
 * lifecycle as the other effect controllers.
 */
@Singleton
class LoudnessController @Inject constructor() {

    private var enhancer: LoudnessEnhancer? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            enhancer = LoudnessEnhancer(audioSessionId)
            _available.value = true
            // Re-apply whatever state the user had selected before this (re)attach.
            applyState()
        } catch (_: Throwable) {
            enhancer = null
            _available.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        applyState()
    }

    private fun applyState() {
        val fx = enhancer ?: return
        try {
            if (_enabled.value) {
                fx.setTargetGain(TARGET_GAIN_MB)
                fx.enabled = true
            } else {
                fx.enabled = false
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        enhancer?.runCatching {
            enabled = false
            release()
        }
        enhancer = null
        _available.value = false
    }

    private companion object {
        /** Make-up gain applied when normalization is on (~7 dB). */
        const val TARGET_GAIN_MB = 700
    }
}
