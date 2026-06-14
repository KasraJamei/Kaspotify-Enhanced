package com.example.kaspotify.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kaspotify.data.model.Song

private val LosslessGold = Color(0xFFE8C468)
private val HighQualityGreen = Color(0xFF1ED760)

/** Small pill showing the audio format/bitrate, e.g. "Lossless", "320 kbps". Renders nothing if unknown. */
@Composable
fun QualityBadge(song: Song, modifier: Modifier = Modifier) {
    val label = song.qualityLabel
    if (label.isEmpty()) return

    val color = when {
        song.isLossless -> LosslessGold
        (song.bitrateBps ?: 0) >= 256_000 -> HighQualityGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.16f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
