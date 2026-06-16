package com.example.kaspotify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.Greetings
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.SongRow
import com.example.kaspotify.ui.components.TourTarget
import com.example.kaspotify.ui.components.niagaraPage
import com.example.kaspotify.ui.components.tourTarget
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassFillStrong
import com.example.kaspotify.ui.theme.GlassStroke
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val tabs = listOf("Songs", "Albums", "Artists", "Favorites")

enum class SmartPlaylistType(val title: String, val icon: ImageVector) {
    RECENTLY_ADDED("Recently Added", Icons.Filled.History),
    MOST_PLAYED("Most Played", Icons.Filled.Whatshot)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onMore: (Song) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val scope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage
    var sortOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val sortMode = SortMode.entries[sortOrdinal]
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id
    val userName by remember { derivedStateOf { viewModel.settings.value.userName } }
    // A fresh greeting each time Home is entered; re-rolls if the user's name changes.
    val greeting = remember(userName) { Greetings.random(userName) }

    val sortedSongs = remember(songs, sortMode) { sortMode.sort(songs) }
    val sortedFavorites = remember(favorites, sortMode) { sortMode.sort(favorites) }
    // The A–Z fast-scroll index only makes sense when the list is sorted alphabetically by title.
    val alphabetIndex = sortMode == SortMode.TITLE_ASC || sortMode == SortMode.TITLE_DESC

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeWordmark(greeting)
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.tourTarget(TourTarget.SETTINGS)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(Modifier.fillMaxWidth().tourTarget(TourTarget.TABS)) {
            PillTabs(selectedTab) { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }
        }

        // Sort/filter applies to the song lists (Songs & Favorites).
        if (selectedTab == 0 || selectedTab == 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .tourTarget(TourTarget.SORT),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortPill(current = sortMode, onSelect = { sortOrdinal = it.ordinal })
            }
        }

        // Swipe horizontally to move between Songs / Albums / Artists / Favorites. This nested pager
        // consumes the horizontal gesture, so swiping a category no longer falls through to the
        // top-level Home / Search / Playlists pager.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it }
        ) { page ->
            Box(Modifier.fillMaxSize().niagaraPage(pagerState, page)) {
                when (page) {
                    0 -> SongList(sortedSongs, currentId, viewModel, onMore, showAlphabetIndex = alphabetIndex)
                    1 -> AlbumList(albums) { album -> onOpenAlbum(album.id) }
                    2 -> ArtistList(artists) { artist -> onOpenArtist(artist.name) }
                    else -> SongList(
                        sortedFavorites, currentId, viewModel, onMore,
                        emptyText = "No favorites yet", showAlphabetIndex = alphabetIndex
                    )
                }
            }
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
private fun HomeWordmark(greeting: String) {
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
        if (greeting.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                greeting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    emptyText: String = "No songs found",
    showAlphabetIndex: Boolean = false
) {
    if (songs.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
        if (showAlphabetIndex && songs.size > 10) {
            AlphabetIndex(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
            ) { letter ->
                val target = songs.indexOfFirst { sectionLetter(it.title) == letter }
                if (target >= 0) scope.launch { listState.scrollToItem(target) }
            }
        }
    }
}

/** Maps a title to its index "section": A–Z for alphabetic, '#' for everything else. */
private fun sectionLetter(title: String): Char {
    val c = title.trimStart().firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * A vertical #/A–Z fast-scroll rail pinned to the right edge. Tap or drag along it to jump the list
 * to the first song in that letter's section. While dragging, the touched letter pops and a large
 * preview bubble floats alongside the finger.
 */
@Composable
private fun AlphabetIndex(
    modifier: Modifier = Modifier,
    onLetter: (Char) -> Unit
) {
    val letters = remember { listOf('#') + ('A'..'Z').toList() }
    var heightPx by remember { mutableIntStateOf(1) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var shownLetter by remember { mutableStateOf('#') }
    var touchY by remember { mutableFloatStateOf(0f) }

    fun pickAt(y: Float) {
        touchY = y.coerceIn(0f, heightPx.toFloat())
        val idx = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[idx]
        if (letter != activeLetter) {
            activeLetter = letter
            shownLetter = letter
            onLetter(letter)
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (activeLetter != null) GlassFillStrong else GlassFill)
                .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> pickAt(offset.y) },
                        onDragEnd = { activeLetter = null },
                        onDragCancel = { activeLetter = null },
                        onVerticalDrag = { change, _ -> pickAt(change.position.y) }
                    )
                }
                .pointerInput(Unit) {
                    // Use onPress/tryAwaitRelease so a tap highlights the letter while held and then
                    // clears on lift — otherwise a tapped letter (and its bubble) stayed stuck until
                    // the next drag.
                    detectTapGestures(
                        onPress = { offset ->
                            pickAt(offset.y)
                            tryAwaitRelease()
                            activeLetter = null
                        }
                    )
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { ch ->
                val active = ch == activeLetter
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.8f else 1f,
                    label = "letterScale"
                )
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                )
            }
        }

        // Floating preview bubble that follows the finger, just left of the rail.
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        x = -(24.dp.roundToPx() + 10.dp.roundToPx()),
                        y = (touchY - 28.dp.toPx()).roundToInt().coerceAtLeast(0)
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shownLetter.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
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
