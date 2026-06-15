package com.example.kaspotify.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke

private val LosslessGold = Color(0xFFE8C468)
private val LosslessSheen = Color(0xFFFFF6D0)

/** Small glass pill showing the audio format/bitrate, e.g. "Lossless", "320 kbps". */
@Composable
fun QualityBadge(song: Song, modifier: Modifier = Modifier) {
    val label = song.qualityLabel
    if (label.isEmpty()) return

    val shape = RoundedCornerShape(6.dp)

    if (song.isLossless) {
        // True lossless gets a slow gold sheen that sweeps across the text, plus a softly
        // pulsing gold border — a quiet "premium" cue.
        val transition = rememberInfiniteTransition(label = "lossless")
        val sweep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheen"
        )
        val glow by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        val startX = sweep * 150f - 40f
        val sheenBrush = Brush.linearGradient(
            colors = listOf(LosslessGold, LosslessSheen, LosslessGold),
            start = Offset(startX, 0f),
            end = Offset(startX + 55f, 22f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.merge(TextStyle(brush = sheenBrush)),
            modifier = modifier
                .clip(shape)
                .background(LosslessGold.copy(alpha = 0.10f))
                .border(1.dp, LosslessGold.copy(alpha = glow), shape)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        )
        return
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
