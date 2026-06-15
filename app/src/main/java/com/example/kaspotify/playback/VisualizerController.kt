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

    // Precomputed log-spaced band edges over the usable FFT bins.
    private val bandEdges: IntArray = buildBandEdges()

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
            val norm = (ln(1f + avg) / LOG_REF).coerceIn(0f, 1f)
            out[b] = norm.pow(0.85f)
        }
        return out
    }

    private fun buildBandEdges(): IntArray {
        val bins = CAPTURE_SIZE / 2
        val lowK = 1
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
    }

    companion object {
        const val NUM_BARS = 32
        private const val CAPTURE_SIZE = 512
        // ln(1 + maxMagnitude) reference; max byte magnitude ~ hypot(127,127) ≈ 180.
        private val LOG_REF = ln(1f + 180f)
    }
}
