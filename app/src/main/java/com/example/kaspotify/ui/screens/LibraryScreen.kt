package com.example.kaspotify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.SongRow

private val tabs = listOf("Songs", "Albums", "Artists", "Favorites")

enum class SmartPlaylistType(val title: String, val icon: ImageVector) {
    RECENTLY_ADDED("Recently Added", Icons.Filled.History),
    MOST_PLAYED("Most Played", Icons.Filled.Whatshot)
}

@Composable
private fun SmartPlaylistRow(onOpen: (SmartPlaylistType) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items(SmartPlaylistType.entries.toList(), key = { it.name }) { type ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onOpen(type) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(type.title, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onMore: (Song) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Your Library", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { viewModel.shuffleAll(songs) }, enabled = songs.isNotEmpty()) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Shuffle")
            }
        }

        SmartPlaylistRow(onOpenSmartPlaylist)

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> SongList(songs, currentId, viewModel, onMore)
            1 -> AlbumList(albums) { album -> onOpenAlbum(album.id) }
            2 -> ArtistList(artists) { artist -> onOpenArtist(artist.name) }
            else -> SongList(favorites, currentId, viewModel, onMore, emptyText = "No favorites yet")
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    currentId: Long?,
    viewModel: MusicViewModel,
    onMore: (Song) -> Unit,
    emptyText: String = "No songs found"
) {
    if (songs.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentId,
                onClick = { viewModel.playSong(song, songs) },
                onToggleFavorite = { viewModel.toggleFavorite(song) },
                onMore = { onMore(song) },
                onPlayNext = { viewModel.playNext(song) },
                onAddToQueue = { viewModel.addToQueue(song) }
            )
        }
    }
}

@Composable
private fun AlbumList(albums: List<Album>, onClick: (Album) -> Unit) {
    if (albums.isEmpty()) {
        EmptyState("No albums found")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums, key = { it.id }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(album) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(uri = album.artworkUri, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        album.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${album.artist} • ${album.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>, onClick: (Artist) -> Unit) {
    if (artists.isEmpty()) {
        EmptyState("No artists found")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(artist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${artist.albumCount} albums • ${artist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
