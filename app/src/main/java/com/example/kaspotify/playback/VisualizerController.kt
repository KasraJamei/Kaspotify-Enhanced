package com.example.kaspotify.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow

/**
 * Owns a [Visualizer] bound to the ExoPlayer audio session. Captures FFT frames and reduces them to
 * a small set of log-spaced, perceptually-scaled frequency-band levels (0..1). Bar motion smoothing
 * (attack/decay) is done in the UI, so this only publishes fresh targets.
 */
@Singleton
class VisualizerController @Inject constructor() {

    private var visualizer: Visualizer? = null
    private var sessionId: Int = 0

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bars = MutableStateFlow(FloatArray(NUM_BARS))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()

    // A 0..1 "beat" signal: how much the current bass exceeds its recent baseline. Spikes on kicks
    // and beats; the UI uses it to pump the whole spectrum so it visibly waves with the bass.
    private val _pulse = MutableStateFlow(0f)
    val pulse: StateFlow<Float> = _pulse.asStateFlow()
    private var bassBaseline = 0f

    // Precomputed log-spaced band edges over the usable FFT bins.
    private val bandEdges: IntArray = buildBandEdges()

    // Per-band exponential moving average, to denoise targets before the UI animates them. The very
    // low bands are single-bin and naturally flicker; this keeps the left side from "twitching".
    private val smoothed = FloatArray(NUM_BARS)

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        sessionId = audioSessionId
    }

    private fun createVisualizer() {
        val audioSessionId = sessionId
        if (audioSessionId == 0 || visualizer != null) return
        try {
            val viz = Visualizer(audioSessionId)
            viz.captureSize = CAPTURE_SIZE.coerceIn(
                Visualizer.getCaptureSizeRange()[0],
                Visualizer.getCaptureSizeRange()[1]
            )
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { _bars.value = computeBars(it) }
                    }
                },
                // A moderate capture rate; the UI interpolates between frames so this need not be fast.
                (Visualizer.getMaxCaptureRate() / 2),
                false,
                true
            )
            visualizer = viz
            _available.value = true
        } catch (t: Throwable) {
            visualizer = null
            _available.value = false
        }
    }

    /** Reduce raw FFT bytes to [NUM_BARS] normalized, perceptually-scaled band levels. */
    private fun computeBars(fft: ByteArray): FloatArray {
        val out = FloatArray(NUM_BARS)
        for (b in 0 until NUM_BARS) {
            val start = bandEdges[b]
            val end = bandEdges[b + 1]
            var sum = 0f
            var count = 0
            var k = start
            while (k < end) {
                val real = fft[2 * k].toFloat()
                val imag = fft[2 * k + 1].toFloat()
                sum += hypot(real, imag)
                count++
                k++
            }
            val avg = if (count > 0) sum / count else 0f
            // Perceptual (roughly log) scaling + gentle gain so quiet detail is still visible.
            var norm = (ln(1f + avg) / LOG_REF).coerceIn(0f, 1f).pow(0.85f)
            // Spectral tilt: bass is far louder than the rest and visually dominates, so ease the
            // lowest bands down toward a flatter, calmer response.
            val tilt = BASS_TILT + (1f - BASS_TILT) * (b.toFloat() / (NUM_BARS - 1))
            norm *= tilt
            // Temporal EMA — denoises the jittery low bins before the UI's attack/decay sees them.
            val prev = smoothed[b]
            val s = if (norm >= prev) {
                // Rise responsively but not instantly.
                prev + (norm - prev) * SMOOTH_RISE
            } else {
                prev + (norm - prev) * SMOOTH_FALL
            }
            smoothed[b] = s
            out[b] = s
        }
        updatePulse(out)
        return out
    }

    /** Derive the beat pulse from how far the current bass sits above its slow-moving baseline. */
    private fun updatePulse(levels: FloatArray) {
        var bass = 0f
        val n = BASS_BANDS.coerceAtMost(levels.size)
        for (i in 0 until n) bass += levels[i]
        bass = if (n > 0) bass / n else 0f
        // Slow baseline so sustained bass doesn't keep the pump maxed; only transients stand out.
        bassBaseline += (bass - bassBaseline) * BASELINE_EMA
        val excess = ((bass - bassBaseline) * PULSE_GAIN).coerceIn(0f, 1f)
        // Fast attack, gentle inter-frame decay, so each beat reads as a distinct pump.
        _pulse.value = if (excess > _pulse.value) excess else _pulse.value * PULSE_DECAY
    }

    private fun buildBandEdges(): IntArray {
        val bins = CAPTURE_SIZE / 2
        // Skip the lowest couple of bins (DC offset / sub-bass rumble) — they're the worst offenders
        // for the "twitching" leftmost bars and carry little musical information.
        val lowK = 3
        val highK = bins - 1
        val edges = IntArray(NUM_BARS + 1)
        for (i in 0..NUM_BARS) {
            val frac = i.toFloat() / NUM_BARS
            val k = (lowK * (highK.toFloat() / lowK).pow(frac)).toInt()
            edges[i] = k.coerceIn(lowK, highK)
        }
        // Ensure strictly increasing so every band has at least one bin.
        for (i in 1..NUM_BARS) {
            if (edges[i] <= edges[i - 1]) edges[i] = (edges[i - 1] + 1).coerceAtMost(highK)
        }
        return edges
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) createVisualizer()
        val viz = visualizer ?: run {
            _enabled.value = false
            return
        }
        try {
            viz.enabled = enabled
            _enabled.value = viz.enabled
            if (!enabled) _bars.value = FloatArray(NUM_BARS)
        } catch (_: Throwable) {
            _enabled.value = false
        }
    }

    fun release() {
        visualizer?.runCatching {
            enabled = false
            release()
        }
        visualizer = null
        _available.value = false
        _enabled.value = false
        _bars.value = FloatArray(NUM_BARS)
        _pulse.value = 0f
        smoothed.fill(0f)
        bassBaseline = 0f
    }

    companion object {
        const val NUM_BARS = 32
        // Larger capture = more FFT bins = finer, less-noisy low/mid resolution.
        private const val CAPTURE_SIZE = 1024
        // ln(1 + maxMagnitude) reference; max byte magnitude ~ hypot(127,127) ≈ 180.
        private val LOG_REF = ln(1f + 180f)
        // How much the lowest band is attenuated relative to the top (0..1). 0.8 keeps the bass
        // present and lively (the DC-bin skip + EMA already handle the old "twitching").
        private const val BASS_TILT = 0.8f
        // EMA factors applied to incoming targets (per capture frame, ~30/s).
        private const val SMOOTH_RISE = 0.6f
        private const val SMOOTH_FALL = 0.35f
        // Beat-pulse tuning.
        private const val BASS_BANDS = 4          // lowest bands averaged into the "bass" measure
        private const val BASELINE_EMA = 0.04f    // how fast the bass baseline tracks the average
        private const val PULSE_GAIN = 3.2f       // sensitivity of the pump to bass transients
        private const val PULSE_DECAY = 0.86f     // per-frame falloff of the pump between beats
    }
}
