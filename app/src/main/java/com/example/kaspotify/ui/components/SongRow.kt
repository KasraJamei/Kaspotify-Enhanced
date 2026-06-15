package com.example.kaspotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.LocalAppSettings
import com.example.kaspotify.ui.theme.GlassFill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    note: String? = null,
    modifier: Modifier = Modifier
) {
    // Swipe-to-queue/play-next is an opt-out feature controlled from Settings.
    if (!LocalAppSettings.current.listSwipeGestures) {
        SongRowContent(song, isCurrent, note, onClick, onToggleFavorite, onMore, modifier)
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onAddToQueue()
                SwipeToDismissBoxValue.EndToStart -> onPlayNext()
                SwipeToDismissBoxValue.Settled -> {}
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            // Only paint a glass reveal while actually swiping; otherwise stay transparent so the
            // gradient backdrop shows through the list.
            val (icon, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.QueueMusic to Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.QueuePlayNext to Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> null to Alignment.Center
            }
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassFill)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) {
        SongRowContent(song, isCurrent, note, onClick, onToggleFavorite, onMore)
    }
}

@Composable
private fun SongRowContent(
    song: Song,
    isCurrent: Boolean,
    note: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val rowShape = RoundedCornerShape(16.dp)
        val highlight = if (isCurrent) GlassFill else androidx.compose.ui.graphics.Color.Transparent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clip(rowShape)
                .background(highlight)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(uri = song.artworkUri, size = 50.dp, cornerRadius = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.qualityLabel.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        QualityBadge(song)
                    }
                }
                if (note != null) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
