package com.example.kaspotify.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.exp

/**
 * Frequency-band visualizer with smooth, natural motion: bars rise quickly toward the latest target
 * and fall under "gravity", instead of snapping. Driven by a single per-frame loop (no per-bar
 * animations), so it stays cheap and never looks jittery/too fast.
 */
@Composable
fun VisualizerView(
    bars: StateFlow<FloatArray>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    // Displayed levels; updated every frame, which redraws the Canvas. We read the StateFlow's
    // current value directly in the frame loop so incoming bar frames never trigger recomposition.
    var levels by remember { mutableStateOf(FloatArray(1)) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                val tgt = bars.value
                val n = tgt.size.coerceAtLeast(1)
                val cur = if (levels.size == n) levels else FloatArray(n)
                val next = FloatArray(n)
                for (i in 0 until n) {
                    val to = if (i < tgt.size) tgt[i] else 0f
                    val c = cur[i]
                    next[i] = if (to >= c) {
                        // Attack: ease up toward the target quickly.
                        c + (to - c) * (1f - exp(-dt * ATTACK))
                    } else {
                        // Decay: fall slowly under gravity, never below the target.
                        (c - dt * DECAY).coerceAtLeast(to)
                    }
                }
                levels = next
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val arr = levels
        val n = arr.size
        if (n == 0) return@Canvas
        val slot = size.width / n
        val barWidth = slot * 0.55f
        val radius = CornerRadius(barWidth / 2, barWidth / 2)
        val maxH = size.height
        val minH = barWidth // keep a little dot even when silent
        for (i in 0 until n) {
            val level = arr[i].coerceIn(0f, 1f)
            val h = (minH + level * (maxH - minH)).coerceAtMost(maxH)
            val x = i * slot + (slot - barWidth) / 2f
            val top = maxH - h
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.45f)),
                    startY = top,
                    endY = maxH
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, h),
                cornerRadius = radius
            )
        }
    }
}

private const val ATTACK = 22f // higher = snappier rise
private const val DECAY = 1.4f // units per second the bars fall
