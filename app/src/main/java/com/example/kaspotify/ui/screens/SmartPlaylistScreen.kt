package com.example.kaspotify.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.GradientBackdrop
import com.example.kaspotify.ui.components.GlassTopBar
import com.example.kaspotify.ui.components.SongRow

/** Generic full-screen song list, used for the "Recently Added" / "Most Played" smart playlists. */
@Composable
fun SmartPlaylistScreen(
    title: String,
    songs: List<Song>,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onMore: (Song) -> Unit,
    note: ((Song) -> String?)? = null,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id

    GradientBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GlassTopBar(title = title, onBack = onBack, actions = {
                IconButton(
                    onClick = { viewModel.shuffleAll(songs) },
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                }
            })

            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    items(songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentId,
                            onClick = { viewModel.playSong(song, songs) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onMore = { onMore(song) },
                            onPlayNext = { viewModel.playNext(song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            note = note?.invoke(song)
                        )
                    }
                }
            }
        }
    }
}
