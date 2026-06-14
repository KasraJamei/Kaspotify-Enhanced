package com.example.kaspotify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.SongRow

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val album = albums.firstOrNull { it.id == albumId }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albumSongs = songs.filter { it.albumId == albumId }
        .sortedWith(compareBy({ it.track }, { it.title }))
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                album?.title ?: "Album",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Artwork(uri = album?.artworkUri, size = 200.dp, cornerRadius = 10.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        album?.title ?: "Album",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${album?.artist ?: ""} • ${albumSongs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = { viewModel.play(albumSongs, 0) },
                            enabled = albumSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick = { viewModel.shuffleAll(albumSongs) },
                            enabled = albumSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }
            items(albumSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentId,
                    onClick = { viewModel.playSong(song, albumSongs) },
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    onMore = { onMore(song) },
                    onPlayNext = { viewModel.playNext(song) },
                    onAddToQueue = { viewModel.addToQueue(song) }
                )
            }
        }
    }
}
