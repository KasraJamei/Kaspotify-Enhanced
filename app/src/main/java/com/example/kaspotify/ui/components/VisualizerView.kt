package com.example.kaspotify.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

private const val BAR_COUNT = 32

/**
 * Animated bar visualizer driven by raw PCM waveform bytes from [VisualizerController].
 * Smoothly interpolates between frames so bars don't jump abruptly.
 */
@Composable
fun VisualizerView(
    waveform: ByteArray,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val levels = remember { List(BAR_COUNT) { Animatable(0f) } }

    LaunchedEffect(waveform) {
        if (waveform.isEmpty()) return@LaunchedEffect
        val samplesPerBar = (waveform.size / BAR_COUNT).coerceAtLeast(1)
        for (i in 0 until BAR_COUNT) {
            var sum = 0f
            val start = i * samplesPerBar
            val end = (start + samplesPerBar).coerceAtMost(waveform.size)
            for (j in start until end) {
                // Unsigned 8-bit PCM is centered at 128; distance from center = amplitude.
                val amplitude = kotlin.math.abs((waveform[j].toInt() and 0xFF) - 128)
                sum += amplitude
            }
            val avg = if (end > start) sum / (end - start) else 0f
            val normalized = (avg / 128f).coerceIn(0f, 1f)
            levels[i].animateTo(normalized, animationSpec = tween(durationMillis = 80))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val barWidth = size.width / (BAR_COUNT * 2f)
        val maxBarHeight = size.height
        levels.forEachIndexed { index, level ->
            val x = barWidth + index * barWidth * 2f
            val barHeight = (0.08f + level.value * 0.92f) * maxBarHeight
            drawLine(
                color = color,
                start = Offset(x, maxBarHeight),
                end = Offset(x, maxBarHeight - barHeight),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
