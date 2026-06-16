package com.example.kaspotify.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.components.GradientBackdrop
import com.example.kaspotify.ui.components.LocalTour
import com.example.kaspotify.ui.components.MiniPlayer
import com.example.kaspotify.ui.components.TourOverlay
import com.example.kaspotify.ui.components.TourState
import com.example.kaspotify.ui.components.TourTarget
import com.example.kaspotify.ui.components.niagaraPage
import com.example.kaspotify.ui.components.tourTarget
import com.example.kaspotify.ui.screens.AlbumDetailScreen
import com.example.kaspotify.ui.screens.ArtistDetailScreen
import com.example.kaspotify.ui.screens.EffectsScreen
import com.example.kaspotify.ui.screens.EqualizerScreen
import com.example.kaspotify.ui.screens.LibraryScreen
import com.example.kaspotify.ui.screens.NowPlayingScreen
import com.example.kaspotify.ui.screens.OnboardingScreen
import com.example.kaspotify.ui.screens.PlaylistDetailScreen
import com.example.kaspotify.ui.screens.PlaylistsScreen
import com.example.kaspotify.ui.screens.QualityScreen
import com.example.kaspotify.ui.screens.QueueScreen
import com.example.kaspotify.ui.screens.SearchScreen
import com.example.kaspotify.ui.screens.SettingsScreen
import com.example.kaspotify.ui.screens.SmartPlaylistScreen
import com.example.kaspotify.ui.screens.SmartPlaylistType
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassFillStrong
import com.example.kaspotify.ui.theme.GlassStroke

private enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    PLAYLISTS("Playlists", Icons.Filled.PlaylistPlay)
}

private val overlayEnter = slideInVertically(
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
    initialOffsetY = { it }
) + fadeIn(tween(200))
private val overlayExit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(150))

/** How long an action toast stays on screen before auto-dismissing. */
private const val NOTIFICATION_MS = 1400L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppScaffold(viewModel: MusicViewModel) {
    val tabsList = Tab.entries
    val pagerState = rememberPagerState(initialPage = 0) { tabsList.size }
    val scope = rememberCoroutineScope()
    val selectedTab = tabsList[pagerState.currentPage]
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var openedAlbumId by remember { mutableStateOf<Long?>(null) }
    var openedArtistName by remember { mutableStateOf<String?>(null) }
    var openedSmartPlaylist by remember { mutableStateOf<SmartPlaylistType?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showEffects by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var moreSong by remember { mutableStateOf<Song?>(null) }

    // Only currentSong is read here (changes once per track). isPlaying/position/duration are
    // collected inside DockedMiniPlayer so their frequent ticks don't recompose this whole tree.
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    val onMore: (Song) -> Unit = { moreSong = it }
    val toastsEnabled = LocalAppSettings.current.inAppToasts

    // Interactive coach-mark tour state (provided to the tree via LocalTour below). It auto-starts
    // once, right after onboarding is dismissed, and can be replayed from Settings.
    val tour = remember { TourState() }
    val onboardingSeen = LocalAppSettings.current.onboardingSeen
    val tourSeen = LocalAppSettings.current.tourSeen
    LaunchedEffect(onboardingSeen, tourSeen) {
        if (onboardingSeen && !tourSeen && !tour.active) {
            delay(450) // let the first layout settle so target bounds are registered
            tour.start()
        }
    }

    // Transient in-app action confirmations — a short, self-dismissing glass toast. Custom (instead
    // of a Snackbar) so we control the duration: a quick ~1.4s flash, not the ~4s Snackbar default.
    var toastText by remember { mutableStateOf("") }
    var toastKind by remember { mutableStateOf(ToastKind.GENERIC) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastSeq by remember { mutableIntStateOf(0) }
    LaunchedEffect(toastsEnabled) {
        if (!toastsEnabled) { toastVisible = false; return@LaunchedEffect }
        viewModel.messages.collect { event ->
            toastText = event.text
            toastKind = event.kind
            toastVisible = true
            toastSeq++
        }
    }
    LaunchedEffect(toastSeq) {
        if (toastVisible) {
            delay(NOTIFICATION_MS)
            toastVisible = false
        }
    }

    CompositionLocalProvider(LocalTour provides tour) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed gradient backdrop sits behind everything.
        GradientBackdrop {}

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                // Floating mini-player + glass nav bar, pinned above system insets.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    currentSong?.let { song ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.widthIn(max = 640.dp).tourTarget(TourTarget.MINIPLAYER)) {
                                DockedMiniPlayer(
                                    viewModel = viewModel,
                                    song = song,
                                    onClick = { showNowPlaying = true }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = 640.dp).tourTarget(TourTarget.NAV)) {
                            FloatingNavBar(
                                selected = selectedTab,
                                onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } }
                            )
                        }
                    }
                }
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Swipe horizontally to move between Home / Search / Playlists. Swiping is disabled
                // while an in-tab detail screen (album/artist/playlist) is open so it doesn't fight
                // that screen's own gestures.
                val detailOpen = (selectedTab == Tab.LIBRARY && (openedAlbumId != null || openedArtistName != null)) ||
                    (selectedTab == Tab.PLAYLISTS && openedPlaylistId != null)
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !detailOpen,
                    modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
                    key = { tabsList[it].name }
                ) { page ->
                    Box(Modifier.fillMaxSize().niagaraPage(pagerState, page)) {
                        when (tabsList[page]) {
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
                                        onOpenArtist = { openedArtistName = it },
                                        onOpenSettings = { showSettings = true }
                                    )
                                }
                            }
                            Tab.SEARCH -> SearchScreen(viewModel, onMore)
                            Tab.PLAYLISTS -> {
                                val id = openedPlaylistId
                                if (id == null) {
                                    PlaylistsScreen(
                                        viewModel = viewModel,
                                        onOpenPlaylist = { openedPlaylistId = it },
                                        onOpenSmartPlaylist = { openedSmartPlaylist = it },
                                        onOpenQuality = { showQuality = true }
                                    )
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
            }
        }

        // Full Now Playing overlay slides up over everything.
        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = overlayEnter,
            exit = overlayExit
        ) {
            BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
            NowPlayingScreen(
                viewModel = viewModel,
                onCollapse = { showNowPlaying = false },
                onOpenEqualizer = { showEqualizer = true },
                onOpenEffects = { showEffects = true },
                onOpenQueue = { showQueue = true }
            )
        }

        AnimatedVisibility(visible = showEqualizer, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = showEqualizer) { showEqualizer = false }
            EqualizerScreen(viewModel = viewModel, onCollapse = { showEqualizer = false })
        }

        AnimatedVisibility(visible = showEffects, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = showEffects) { showEffects = false }
            EffectsScreen(viewModel = viewModel, onCollapse = { showEffects = false })
        }

        AnimatedVisibility(visible = showQueue, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = showQueue) { showQueue = false }
            QueueScreen(viewModel = viewModel, onCollapse = { showQueue = false })
        }

        // Smart-playlist list overlay (Recently Added / Most Played).
        AnimatedVisibility(visible = openedSmartPlaylist != null, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = openedSmartPlaylist != null) { openedSmartPlaylist = null }
            val type = openedSmartPlaylist
            if (type != null) {
                val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
                val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
                SmartPlaylistScreen(
                    title = type.title,
                    songs = when (type) {
                        SmartPlaylistType.RECENTLY_ADDED -> recentlyAdded
                        SmartPlaylistType.MOST_PLAYED -> mostPlayed
                    },
                    viewModel = viewModel,
                    onBack = { openedSmartPlaylist = null },
                    onMore = onMore,
                    note = if (type == SmartPlaylistType.MOST_PLAYED) { song ->
                        song.weeklyPlayCount.takeIf { it > 0 }?.let { n ->
                            if (n == 1) "Listened once this week" else "Listened $n times this week"
                        }
                    } else null
                )
            }
        }

        AnimatedVisibility(visible = showQuality, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = showQuality) { showQuality = false }
            QualityScreen(viewModel = viewModel, onBack = { showQuality = false }, onMore = onMore)
        }

        AnimatedVisibility(visible = showSettings, enter = overlayEnter, exit = overlayExit) {
            BackHandler(enabled = showSettings) { showSettings = false }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { showSettings = false },
                onReplayTour = {
                    showSettings = false
                    scope.launch { pagerState.animateScrollToPage(0) }
                    tour.start()
                }
            )
        }

        // Toast lives at the very top of the stack so confirmations are visible even when the
        // full-screen Now Playing / Equalizer overlays are open.
        AnimatedVisibility(
            visible = toastVisible,
            enter = fadeIn(tween(140)) + slideInVertically { it / 2 } + scaleIn(initialScale = 0.85f, animationSpec = tween(200)),
            exit = fadeOut(tween(200)) + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.9f, animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp)
        ) {
            GlassSnackbar(toastText, toastKind, toastSeq)
        }

        // Coach-mark tour sits above the app chrome. It never runs at the same time as onboarding,
        // which is drawn last (on top). Tapping the dim area or Skip/Done dismisses it.
        TourOverlay(tour) {
            viewModel.setTourSeen(true)
            tour.stop()
        }

        // First-launch welcome guide sits above everything until dismissed.
        AnimatedVisibility(
            visible = !LocalAppSettings.current.onboardingSeen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250))
        ) {
            OnboardingScreen(onFinish = { viewModel.setOnboardingSeen(true) })
        }
    }
    }

    moreSong?.let { song ->
        MoreSheet(song = song, viewModel = viewModel, onDismiss = { moreSong = null })
    }
}

/**
 * Wraps [MiniPlayer] and collects the frequently-changing playback state (isPlaying / position /
 * duration) here, so the 500ms position ticks only recompose this small subtree — not AppScaffold
 * and every list behind it.
 */
@Composable
private fun DockedMiniPlayer(viewModel: MusicViewModel, song: Song, onClick: () -> Unit) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    MiniPlayer(
        song = song,
        isPlaying = isPlaying,
        progress = progress,
        onTogglePlay = viewModel::togglePlayPause,
        onToggleLike = { viewModel.toggleFavorite(song) },
        onClick = onClick
    )
}

@Composable
private fun GlassSnackbar(message: String, kind: ToastKind, seq: Int) {
    val shape = RoundedCornerShape(16.dp)
    // A small confirmation pill, centered and only as wide as its content so it reads as a distinct
    // notification rather than a full-width bar. The icon changes per action and pops on each new
    // toast so it always feels alive.
    val icon = when (kind) {
        ToastKind.QUEUE -> Icons.Filled.QueueMusic
        ToastKind.PLAY_NEXT -> Icons.Filled.QueuePlayNext
        ToastKind.LIKE -> Icons.Filled.Favorite
        ToastKind.UNLIKE -> Icons.Filled.HeartBroken
        ToastKind.PLAYLIST -> Icons.Filled.PlaylistAdd
        ToastKind.TIMER -> Icons.Filled.Bedtime
        ToastKind.GENERIC -> Icons.Filled.Check
    }
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(seq) {
        pop.snapTo(0.5f)
        pop.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), shape)
                .padding(start = 12.dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FloatingNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .height(60.dp)
            .clip(shape)
            .background(GlassFillStrong)
            .border(1.dp, GlassStroke, shape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.entries.forEach { tab ->
            NavItem(
                tab = tab,
                selected = selected == tab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) GlassFill else Color.Transparent,
        animationSpec = tween(220),
        label = "navPill"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "navContent"
    )
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(pillColor)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        if (selected) {
            Spacer(Modifier.size(8.dp))
            Text(tab.label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(song: Song, viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var pickingPlaylist by remember { mutableStateOf(false) }
    var showingDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text(song.artist) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            if (showingDetails) {
                SongDetails(song)
            } else if (!pickingPlaylist) {
                SheetAction(Icons.Filled.QueuePlayNext, "Play next") {
                    viewModel.playNext(song); onDismiss()
                }
                SheetAction(Icons.Filled.QueueMusic, "Add to queue") {
                    viewModel.addToQueue(song); onDismiss()
                }
                SheetAction(Icons.Filled.PlaylistAdd, "Add to playlist") {
                    pickingPlaylist = true
                }
                SheetAction(Icons.Filled.Share, "Share") {
                    shareSong(context, song); onDismiss()
                }
                SheetAction(Icons.Filled.Info, "Details") {
                    showingDetails = true
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
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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

/**
 * Shares the track's audio file to other apps via the system chooser. Uses the song's MediaStore
 * content URI as the stream and grants temporary read permission to the receiving app, with a
 * "Title — Artist" text/subject fallback for apps that only take text.
 */
private fun shareSong(context: android.content.Context, song: Song) {
    val caption = "${song.title} — ${song.artist}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = song.mimeType.ifEmpty { "audio/*" }
        putExtra(Intent.EXTRA_STREAM, song.uri)
        putExtra(Intent.EXTRA_SUBJECT, caption)
        putExtra(Intent.EXTRA_TITLE, song.title)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Share song").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SongDetails(song: Song) {
    val rows = buildList {
        add("Title" to song.title)
        add("Artist" to song.artist)
        add("Album" to song.album)
        if (song.year > 0) add("Year" to song.year.toString())
        val mins = song.durationMs / 60000
        val secs = (song.durationMs / 1000) % 60
        add("Duration" to String.format(java.util.Locale.US, "%d:%02d", mins, secs))
        song.qualityLabel.takeIf { it.isNotEmpty() }?.let { add("Quality" to it) }
        song.bitrateBps?.let { add("Bitrate" to "${it / 1000} kbps") }
        song.mimeType.takeIf { it.isNotEmpty() }?.let { add("Format" to it) }
        song.filePath.takeIf { it.isNotEmpty() }?.let { add("Path" to it) }
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        rows.forEach { (label, value) ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}
