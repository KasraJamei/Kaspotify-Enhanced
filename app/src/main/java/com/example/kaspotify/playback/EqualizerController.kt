package com.example.kaspotify.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class EqualizerBand(
    val index: Int,
    val centerFreqMilliHz: Int,
    val level: Short
)

/** How the equalizer is presented: a friendly 3-knob view or the full per-band view. */
enum class EqMode { SIMPLE, ADVANCED }

/** The three coarse controls in Simple mode, each mapped onto one or more hardware bands. */
enum class SimpleBand { BASS, MID, TREBLE }

/**
 * Owns an [Equalizer] bound to the ExoPlayer audio session. The session id is supplied by
 * [PlaybackService] once the player is created; the UI reads/controls it through this singleton via
 * the ViewModel.
 *
 * Two interaction modes share the same underlying effect:
 *  - **Advanced** drives the device's native bands and presets directly.
 *  - **Simple** exposes Bass / Mid / Treble on a friendly -10..+10 scale; each maps to the hardware
 *    bands grouped by center frequency, so both modes stay in sync.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** [min, max] band level in millibels. */
    private val _levelRange = MutableStateFlow(-1500 to 1500)
    val levelRange: StateFlow<Pair<Int, Int>> = _levelRange.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow(-1)
    val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    /** Simple vs Advanced view. Held here (singleton) so it survives screen recompositions. */
    private val _mode = MutableStateFlow(EqMode.SIMPLE)
    val mode: StateFlow<EqMode> = _mode.asStateFlow()

    /** Current Bass/Mid/Treble positions on a -10..+10 scale (derived from the hardware bands). */
    private val _simpleLevels = MutableStateFlow(
        mapOf(SimpleBand.BASS to 0, SimpleBand.MID to 0, SimpleBand.TREBLE to 0)
    )
    val simpleLevels: StateFlow<Map<SimpleBand, Int>> = _simpleLevels.asStateFlow()

    /** Hardware band indices grouped into Bass/Mid/Treble by center frequency. */
    private var simpleGroups: Map<SimpleBand, List<Int>> = emptyMap()

    /** Called from the service when the audio session id is known. Safe to call repeatedly. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            val eq = Equalizer(EFFECT_PRIORITY, audioSessionId)
            equalizer = eq
            val range = eq.bandLevelRange
            _levelRange.value = range[0].toInt() to range[1].toInt()
            _presets.value = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            _enabled.value = eq.enabled
            simpleGroups = computeSimpleGroups(eq)
            refreshBands()
            _currentPreset.value = eq.currentPreset.toInt()
            _available.value = true
        } catch (t: Throwable) {
            // Equalizer is unavailable on some devices/emulators.
            equalizer = null
            _available.value = false
        }
    }

    fun setMode(mode: EqMode) {
        _mode.value = mode
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.let {
            it.enabled = enabled
            _enabled.value = it.enabled
        }
    }

    // ---- Advanced controls ----

    fun setBandLevel(bandIndex: Int, levelMilliBel: Short) {
        val eq = equalizer ?: return
        try {
            ensureEnabled(eq)
            eq.setBandLevel(bandIndex.toShort(), levelMilliBel)
            _currentPreset.value = -1 // custom
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        try {
            ensureEnabled(eq)
            eq.usePreset(presetIndex.toShort())
            _currentPreset.value = presetIndex
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    // ---- Simple controls (Bass / Mid / Treble on a -10..+10 scale) ----

    /** Set one coarse band; applies the matching millibel level to every hardware band it covers. */
    fun setSimpleLevel(band: SimpleBand, value: Int) {
        val eq = equalizer ?: return
        val clamped = value.coerceIn(-SIMPLE_RANGE, SIMPLE_RANGE)
        val level = simpleToMilliBel(clamped)
        try {
            ensureEnabled(eq)
            simpleGroups[band]?.forEach { idx -> eq.setBandLevel(idx.toShort(), level) }
            _currentPreset.value = -1 // custom
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    /** Flatten every band to 0 dB (used by the Simple-mode reset). */
    fun resetBands() {
        val eq = equalizer ?: return
        try {
            ensureEnabled(eq)
            (0 until eq.numberOfBands).forEach { eq.setBandLevel(it.toShort(), 0) }
            _currentPreset.value = -1
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    /** Adjusting bands/presets only has an audible effect once the effect is enabled. */
    private fun ensureEnabled(eq: Equalizer) {
        if (!eq.enabled) {
            eq.enabled = true
            _enabled.value = true
        }
    }

    fun release() {
        equalizer?.runCatching { release() }
        equalizer = null
        _available.value = false
    }

    private fun refreshBands() {
        val eq = equalizer ?: return
        val bands = (0 until eq.numberOfBands).map { i ->
            val band = i.toShort()
            EqualizerBand(
                index = i,
                centerFreqMilliHz = eq.getCenterFreq(band),
                level = eq.getBandLevel(band)
            )
        }
        _bands.value = bands
        // Keep the Simple knobs reflecting reality (e.g. after a preset or advanced edit).
        _simpleLevels.value = simpleGroups.mapValues { (_, indices) ->
            if (indices.isEmpty()) 0
            else milliBelToSimple(indices.map { bands[it].level.toInt() }.average().roundToInt())
        }
    }

    /** Partition hardware bands into Bass/Mid/Treble by center frequency, with index-thirds fallback. */
    private fun computeSimpleGroups(eq: Equalizer): Map<SimpleBand, List<Int>> {
        val n = eq.numberOfBands.toInt()
        if (n == 0) return emptyMap()
        val bass = mutableListOf<Int>()
        val mid = mutableListOf<Int>()
        val treble = mutableListOf<Int>()
        for (i in 0 until n) {
            val freq = eq.getCenterFreq(i.toShort()) // milliHz
            when {
                freq <= BASS_MAX_MILLIHZ -> bass
                freq >= TREBLE_MIN_MILLIHZ -> treble
                else -> mid
            }.add(i)
        }
        // If the frequency split left a group empty (unusual band layouts), fall back to thirds.
        if (bass.isEmpty() || mid.isEmpty() || treble.isEmpty()) {
            bass.clear(); mid.clear(); treble.clear()
            val third = (n + 2) / 3
            for (i in 0 until n) when {
                i < third -> bass
                i >= n - third -> treble
                else -> mid
            }.add(i)
            // Guarantee mid has at least one band on very small EQs.
            if (mid.isEmpty() && bass.size > 1) mid.add(bass.removeAt(bass.size - 1))
        }
        return mapOf(SimpleBand.BASS to bass, SimpleBand.MID to mid, SimpleBand.TREBLE to treble)
    }

    /** -10..+10 -> millibels within the device's band range. */
    private fun simpleToMilliBel(value: Int): Short {
        val (min, max) = _levelRange.value
        val frac = value.toFloat() / SIMPLE_RANGE
        val mb = if (frac >= 0) frac * max else frac * -min
        return mb.roundToInt().toShort()
    }

    /** millibels -> nearest -10..+10 step. */
    private fun milliBelToSimple(milliBel: Int): Int {
        val (min, max) = _levelRange.value
        val frac = if (milliBel >= 0) milliBel.toFloat() / max else milliBel.toFloat() / -min
        return (frac * SIMPLE_RANGE).roundToInt().coerceIn(-SIMPLE_RANGE, SIMPLE_RANGE)
    }

    companion object {
        private const val EFFECT_PRIORITY = 0
        /** Simple-mode slider extent: -10..+10. */
        const val SIMPLE_RANGE = 10
        // Frequency boundaries for the Bass/Mid/Treble split (in milliHz).
        private const val BASS_MAX_MILLIHZ = 250_000
        private const val TREBLE_MIN_MILLIHZ = 4_000_000
    }
}
