package com.example.kaspotify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Playlist
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.SongRow

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist: Playlist? = playlists.firstOrNull { it.id == playlistId }
    val songs by viewModel.playlistSongs(playlistId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add songs")
            }
            IconButton(
                onClick = { viewModel.shuffleAll(songs) },
                enabled = songs.isNotEmpty()
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
            }
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "This playlist is empty.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.LibraryAdd, contentDescription = "Add songs", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(songs, key = { it.id }) { song ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentId,
                            onClick = { viewModel.playSong(song, songs) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onMore = { onMore(song) },
                            onPlayNext = { viewModel.playNext(song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeFromPlaylist(playlistId, song) }) {
                            Icon(
                                Icons.Filled.RemoveCircleOutline,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddSongsSheet(
            viewModel = viewModel,
            existingIds = songs.mapTo(HashSet()) { it.id },
            onAdd = { viewModel.addToPlaylist(playlistId, it) },
            onDismiss = { showAdd = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsSheet(
    viewModel: MusicViewModel,
    existingIds: Set<Long>,
    onAdd: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val library by viewModel.songs.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var addedIds by remember { mutableStateOf(setOf<Long>()) }

    val candidates = remember(library, existingIds, addedIds, query) {
        val q = query.trim()
        library.asSequence()
            .filter { it.id !in existingIds }
            .filter { q.isEmpty() || it.title.contains(q, true) || it.artist.contains(q, true) }
            .take(200)
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                "Add songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search your library") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(candidates, key = { it.id }) { song ->
                    val added = song.id in addedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !added) {
                                onAdd(song)
                                addedIds = addedIds + song.id
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Artwork(uri = song.artworkUri, size = 44.dp, cornerRadius = 8.dp)
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            if (added) Icons.Filled.LibraryAdd else Icons.Filled.Add,
                            contentDescription = if (added) "Added" else "Add",
                            tint = if (added) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
