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
import com.example.kaspotify.ui.components.SongRow

@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artistSongs = songs.filter { it.artist == artistName }
        .sortedWith(compareBy({ it.album.lowercase() }, { it.track }, { it.title }))
    val albumCount = artistSongs.map { it.albumId }.distinct().size
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
                artistName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "$albumCount albums • ${artistSongs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.play(artistSongs, 0) },
                            enabled = artistSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick = { viewModel.shuffleAll(artistSongs) },
                            enabled = artistSongs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }
            items(artistSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentId,
                    onClick = { viewModel.playSong(song, artistSongs) },
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    onMore = { onMore(song) },
                    onPlayNext = { viewModel.playNext(song) },
                    onAddToQueue = { viewModel.addToQueue(song) }
                )
            }
        }
    }
}
