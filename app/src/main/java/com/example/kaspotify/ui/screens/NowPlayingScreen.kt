package com.example.kaspotify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.playback.RepeatMode
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.QualityBadge
import com.example.kaspotify.ui.components.VisualizerView
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke
import com.example.kaspotify.ui.theme.LocalAmbientColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

private val sleepOptions = listOf<Int?>(null, 15, 30, 45, 60)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenEffects: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val shuffle by viewModel.shuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val visualizerEnabled by viewModel.visualizer.enabled.collectAsStateWithLifecycle()
    val recordAudioPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val current = song
    val ambient = LocalAmbientColor.current
    val base = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .background(
                Brush.verticalGradient(
                    0f to ambient.copy(alpha = 0.85f).compositeOver(base),
                    0.5f to ambient.copy(alpha = 0.30f).compositeOver(base),
                    1f to base
                )
            )
    ) {
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing playing", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Box
        }

        var scrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableStateOf(0f) }
        val effectiveDuration = if (durationMs > 0) durationMs else current.durationMs
        val sliderValue = if (scrubbing) scrubValue
        else if (effectiveDuration > 0) positionMs.toFloat() / effectiveDuration else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                }
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "Queue")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hero artwork
            var heartTrigger by remember { mutableIntStateOf(0) }
            val heartAnim = remember { Animatable(0f) }
            LaunchedEffect(heartTrigger) {
                if (heartTrigger == 0) return@LaunchedEffect
                heartAnim.snapTo(0f)
                heartAnim.animateTo(1f, animationSpec = tween(680, easing = FastOutSlowInEasing))
            }
            var artworkVisible by remember(current.id) { mutableStateOf(false) }
            val artworkScale by animateFloatAsState(
                targetValue = if (artworkVisible) 1f else 0.88f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "artworkEntrance"
            )
            LaunchedEffect(current.id) { artworkVisible = true }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = artworkScale
                        scaleY = artworkScale
                        shadowElevation = 24f
                        shape = RoundedCornerShape(20.dp)
                        clip = false
                    }
                    .pointerInput(current.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!current.isFavorite) viewModel.toggleFavorite(current)
                                heartTrigger++
                            }
                        )
                    }
            ) {
                Artwork(
                    uri = current.artworkUri,
                    size = 360.dp,
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxSize()
                )
                val hp = heartAnim.value
                if (hp > 0f && hp < 1f) {
                    // Expanding ring that ripples outward and fades.
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size((80 + 150 * hp).dp)
                            .border(2.dp, Color.White.copy(alpha = (1f - hp) * 0.5f), CircleShape)
                    )
                    // Heart: overshoot pop, then drift up and fade out.
                    val scale = when {
                        hp < 0.4f -> 0.4f + (1.25f - 0.4f) * (hp / 0.4f)
                        hp < 0.6f -> 1.25f + (1f - 1.25f) * ((hp - 0.4f) / 0.2f)
                        else -> 1f
                    }
                    val heartAlpha = if (hp < 0.7f) 1f else (1f - (hp - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(120.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = heartAlpha
                                translationY = -70f * hp
                            }
                    )
                }
            }

            if (visualizerEnabled) {
                Spacer(Modifier.height(14.dp))
                VisualizerView(
                    bars = viewModel.visualizer.bars,
                    pulse = viewModel.visualizer.pulse,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            // Title + favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        current.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            current.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (current.qualityLabel.isNotEmpty()) {
                            Spacer(Modifier.size(8.dp))
                            QualityBadge(current)
                        }
                    }
                }
                IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                    Icon(
                        imageVector = if (current.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (current.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Seek bar
            Slider(
                value = sliderValue.coerceIn(0f, 1f),
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    scrubbing = false
                    if (effectiveDuration > 0) {
                        viewModel.seekTo((scrubValue * effectiveDuration).toLong())
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = GlassStroke
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(if (scrubbing) (scrubValue * effectiveDuration).toLong() else positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTime(effectiveDuration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // Transport
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = viewModel::previous) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = viewModel::togglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(tween(150)) + fadeIn(tween(150))) togetherWith
                                (scaleOut(tween(150)) + fadeOut(tween(150)))
                        },
                        label = "nowPlayingPlayPause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                IconButton(onClick = viewModel::next) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = viewModel::cycleRepeat) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne
                        else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Secondary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionChip(
                    icon = Icons.Filled.GraphicEq,
                    label = "Visualizer",
                    active = visualizerEnabled,
                    onClick = {
                        if (visualizerEnabled) viewModel.setVisualizerEnabled(false)
                        else if (recordAudioPermission.status.isGranted) viewModel.setVisualizerEnabled(true)
                        else recordAudioPermission.launchPermissionRequest()
                    }
                )
                ActionChip(Icons.Filled.Equalizer, "EQ", false, onOpenEqualizer)
                ActionChip(Icons.Filled.Speed, "Effects", false, onOpenEffects)
                SleepTimerChip(selected = sleepTimer, onSelect = viewModel::setSleepTimer)
            }

            Spacer(Modifier.height(10.dp))
        }

        // Vertical "KASPOTIFY" wordmark down the left edge — a quiet, on-brand signature that
        // fades in letter-by-letter when the screen opens.
        KaspotifyWordmark(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )
    }
}

@Composable
private fun KaspotifyWordmark(modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        WORDMARK.forEachIndexed { i, ch ->
            val alpha by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(durationMillis = 450, delayMillis = 300 + i * 55),
                label = "wordmark"
            )
            Text(
                text = ch.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f * alpha)
            )
        }
    }
}

private const val WORDMARK = "KASPOTIFY"

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(shape)
                .background(GlassFill)
                .border(1.dp, GlassStroke, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun SleepTimerChip(selected: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActionChip(
            icon = Icons.Filled.Bedtime,
            label = selected?.let { "${it}m" } ?: "Timer",
            active = selected != null,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sleepOptions.forEach { minutes ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = minutes?.let { "$it min" } ?: "Off",
                            color = if (minutes == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        onSelect(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
