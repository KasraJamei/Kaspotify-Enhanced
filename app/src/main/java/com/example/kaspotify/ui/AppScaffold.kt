package com.example.kaspotify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.components.MiniPlayer
import com.example.kaspotify.ui.screens.AlbumDetailScreen
import com.example.kaspotify.ui.screens.ArtistDetailScreen
import com.example.kaspotify.ui.screens.EqualizerScreen
import com.example.kaspotify.ui.screens.LibraryScreen
import com.example.kaspotify.ui.screens.NowPlayingScreen
import com.example.kaspotify.ui.screens.PlaylistDetailScreen
import com.example.kaspotify.ui.screens.PlaylistsScreen
import com.example.kaspotify.ui.screens.SearchScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    PLAYLISTS("Playlists", Icons.Filled.PlaylistPlay)
}

@Composable
fun AppScaffold(viewModel: MusicViewModel) {
    var selectedTab by remember { mutableStateOf(Tab.LIBRARY) }
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var openedAlbumId by remember { mutableStateOf<Long?>(null) }
    var openedArtistName by remember { mutableStateOf<String?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var moreSong by remember { mutableStateOf<Song?>(null) }

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()

    val onMore: (Song) -> Unit = { moreSong = it }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    currentSong?.let { song ->
                        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            progress = progress,
                            onTogglePlay = viewModel::togglePlayPause,
                            onClick = { showNowPlaying = true }
                        )
                    }
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    Tab.LIBRARY -> {
                        val albumId = openedAlbumId
                        val artistName = openedArtistName
                        when {
                            albumId != null -> AlbumDetailScreen(
                                albumId = albumId,
                                viewModel = viewModel,
                                onBack = { openedAlbumId = null },
                                onMore = onMore
                            )
                            artistName != null -> ArtistDetailScreen(
                                artistName = artistName,
                                viewModel = viewModel,
                                onBack = { openedArtistName = null },
                                onMore = onMore
                            )
                            else -> LibraryScreen(
                                viewModel = viewModel,
                                onMore = onMore,
                                onOpenAlbum = { openedAlbumId = it },
                                onOpenArtist = { openedArtistName = it }
                            )
                        }
                    }
                    Tab.SEARCH -> SearchScreen(viewModel, onMore)
                    Tab.PLAYLISTS -> {
                        val id = openedPlaylistId
                        if (id == null) {
                            PlaylistsScreen(viewModel, onOpenPlaylist = { openedPlaylistId = it })
                        } else {
                            PlaylistDetailScreen(
                                playlistId = id,
                                viewModel = viewModel,
                                onBack = { openedPlaylistId = null },
                                onMore = onMore
                            )
                        }
                    }
                }
            }
        }

        // Full Now Playing overlay slides up over everything.
        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
            NowPlayingScreen(
                viewModel = viewModel,
                onCollapse = { showNowPlaying = false },
                onOpenEqualizer = { showEqualizer = true }
            )
        }

        // Equalizer overlay on top of Now Playing.
        AnimatedVisibility(
            visible = showEqualizer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = showEqualizer) { showEqualizer = false }
            EqualizerScreen(viewModel = viewModel, onCollapse = { showEqualizer = false })
        }
    }

    moreSong?.let { song ->
        MoreSheet(
            song = song,
            viewModel = viewModel,
            onDismiss = { moreSong = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(song: Song, viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var pickingPlaylist by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text(song.artist) }
            )
            if (!pickingPlaylist) {
                SheetAction(Icons.Filled.QueuePlayNext, "Play next") {
                    viewModel.playNext(song); onDismiss()
                }
                SheetAction(Icons.Filled.QueueMusic, "Add to queue") {
                    viewModel.addToQueue(song); onDismiss()
                }
                SheetAction(Icons.Filled.PlaylistAdd, "Add to playlist") {
                    pickingPlaylist = true
                }
            } else {
                Text(
                    "Add to playlist",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet. Create one from the Playlists tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn {
                        items(playlists, key = { it.id }) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.songCount} songs") },
                                modifier = Modifier.clickable {
                                    viewModel.addToPlaylist(playlist.id, song)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
