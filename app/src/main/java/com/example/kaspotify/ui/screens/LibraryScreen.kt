package com.example.kaspotify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.SongRow
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke
import com.example.kaspotify.ui.theme.LocalAmbientColor

private val tabs = listOf("Songs", "Albums", "Artists", "Favorites")

enum class SmartPlaylistType(val title: String, val icon: ImageVector) {
    RECENTLY_ADDED("Recently Added", Icons.Filled.History),
    MOST_PLAYED("Most Played", Icons.Filled.Whatshot)
}

@Composable
private fun SmartPlaylistCards(onOpen: (SmartPlaylistType) -> Unit) {
    val ambient = LocalAmbientColor.current
    val surface = MaterialTheme.colorScheme.surface
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(SmartPlaylistType.entries.toList(), key = { it.name }) { type ->
            val shape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .height(110.dp)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ambient.copy(alpha = 0.45f).compositeOver(surface),
                                ambient.copy(alpha = 0.12f).compositeOver(surface)
                            )
                        )
                    )
                    .border(1.dp, GlassStroke, shape)
                    .clickable { onOpen(type) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(GlassFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    type.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    var sortOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val sortMode = SortMode.entries[sortOrdinal]
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id

    val sortedSongs = remember(songs, sortMode) { sortMode.sort(songs) }
    val sortedFavorites = remember(favorites, sortMode) { sortMode.sort(favorites) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeWordmark()
            ShuffleButton(enabled = songs.isNotEmpty()) { viewModel.shuffleAll(songs) }
        }

        SmartPlaylistCards(onOpenSmartPlaylist)
        Spacer(Modifier.height(8.dp))

        PillTabs(selectedTab) { selectedTab = it }

        // Sort/filter applies to the song lists (Songs & Favorites).
        if (selectedTab == 0 || selectedTab == 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortPill(current = sortMode, onSelect = { sortOrdinal = it.ordinal })
            }
        }

        when (selectedTab) {
            0 -> SongList(sortedSongs, currentId, viewModel, onMore)
            1 -> AlbumList(albums) { album -> onOpenAlbum(album.id) }
            2 -> ArtistList(artists) { artist -> onOpenArtist(artist.name) }
            else -> SongList(sortedFavorites, currentId, viewModel, onMore, emptyText = "No favorites yet")
        }
    }
}

private enum class SortMode(val label: String) {
    TITLE_ASC("Title A–Z"),
    TITLE_DESC("Title Z–A"),
    ARTIST("Artist"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LONGEST("Longest");

    fun sort(list: List<Song>): List<Song> = when (this) {
        TITLE_ASC -> list.sortedBy { it.title.lowercase() }
        TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
        ARTIST -> list.sortedBy { it.artist.lowercase() }
        NEWEST -> list.sortedByDescending { it.dateAddedSec }
        OLDEST -> list.sortedBy { it.dateAddedSec }
        LONGEST -> list.sortedByDescending { it.durationMs }
    }
}

@Composable
private fun HomeWordmark() {
    Column {
        Text(
            "KASPOTIFY",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SortPill(current: SortMode, onSelect: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(percent = 50)
    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(GlassFill)
                .border(1.dp, GlassStroke, shape)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.SwapVert,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                current.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            mode.label,
                            color = if (mode == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ShuffleButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Shuffle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Shuffle",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PillTabs(selected: Int, onSelect: (Int) -> Unit) {
    androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = selected,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(positions[selected]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        title,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(album) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(uri = album.artworkUri, size = 52.dp, cornerRadius = 10.dp)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(artists, key = { it.name }) { artist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(artist) }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
