package com.example.kaspotify.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.playback.RepeatMode
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.Artwork
import com.example.kaspotify.ui.components.QualityBadge
import com.example.kaspotify.ui.components.VisualizerView
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
    val waveform by viewModel.visualizer.waveform.collectAsStateWithLifecycle()
    val recordAudioPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val current = song

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing playing", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Surface
        }

        // Local scrub state so the thumb tracks the finger smoothly.
        var scrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableStateOf(0f) }
        val effectiveDuration = if (durationMs > 0) durationMs else current.durationMs
        val sliderValue = if (scrubbing) scrubValue
        else if (effectiveDuration > 0) positionMs.toFloat() / effectiveDuration else 0f

        val accent = MaterialTheme.colorScheme.primary
        val backgroundBrush = remember(accent) {
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.25f), Color.Transparent))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                }
                Text("Now Playing", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (visualizerEnabled) {
                            viewModel.setVisualizerEnabled(false)
                        } else if (recordAudioPermission.status.isGranted) {
                            viewModel.setVisualizerEnabled(true)
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    }) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = "Visualizer",
                            tint = if (visualizerEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenEffects) {
                        Icon(Icons.Filled.Speed, contentDescription = "Effects")
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Queue")
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = "Equalizer")
                    }
                    SleepTimerMenu(
                        selected = sleepTimer,
                        onSelect = viewModel::setSleepTimer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            var heartPop by remember { mutableStateOf(false) }
            val heartScale by animateFloatAsState(
                targetValue = if (heartPop) 1f else 0f,
                animationSpec = if (heartPop) spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                else tween(durationMillis = 150),
                label = "heartPop",
                finishedListener = { if (heartPop) heartPop = false }
            )
            var artworkVisible by remember(current.id) { mutableStateOf(false) }
            val artworkScale by animateFloatAsState(
                targetValue = if (artworkVisible) 1f else 0.85f,
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
                    }
                    .pointerInput(current.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!current.isFavorite) viewModel.toggleFavorite(current)
                                heartPop = true
                            }
                        )
                    }
            ) {
                Artwork(
                    uri = current.artworkUri,
                    size = 320.dp,
                    cornerRadius = 12.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (heartScale > 0f) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(96.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                                alpha = heartScale
                            }
                    )
                }
            }
            if (visualizerEnabled) {
                Spacer(Modifier.height(12.dp))
                VisualizerView(waveform = waveform, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        current.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        current.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (current.qualityLabel.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        QualityBadge(current)
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
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
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
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    IconButton(onClick = viewModel::togglePlayPause) {
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
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = viewModel::next) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
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
        }
    }
}

@Composable
private fun SleepTimerMenu(selected: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = "Sleep timer",
                tint = if (selected != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
