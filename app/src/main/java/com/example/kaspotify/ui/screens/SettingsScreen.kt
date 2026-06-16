package com.example.kaspotify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.UpdateState
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onReplayTour: () -> Unit,
    onOpenPatchNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val genreBuilding by viewModel.genreBuilding.collectAsStateWithLifecycle()
    val genreProgress by viewModel.genreProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        var showNameDialog by remember { mutableStateOf(false) }
        SectionLabel("Profile")
        GuideRow(
            icon = Icons.Filled.Person,
            title = settings.userName.ifBlank { "Set your name" },
            subtitle = if (settings.userName.isBlank()) "Personalize your greetings on Home"
            else "Tap to change your name",
            onClick = { showNameDialog = true }
        )
        if (showNameDialog) {
            NameDialog(
                current = settings.userName,
                onConfirm = { viewModel.setUserName(it); showNameDialog = false },
                onDismiss = { showNameDialog = false }
            )
        }

        SectionLabel("Appearance")
        SettingToggle(
            icon = Icons.Filled.Palette,
            title = "Album-art theming",
            subtitle = "Tint backdrops with the current track's colors",
            checked = settings.albumArtTheming,
            onChange = viewModel::setAlbumArtTheming
        )
        SettingToggle(
            icon = Icons.Filled.Bolt,
            title = "High refresh rate",
            subtitle = "Use the smoothest display mode the screen allows",
            checked = settings.highRefreshRate,
            onChange = viewModel::setHighRefreshRate
        )

        SectionLabel("Playback & audio")
        SettingToggle(
            icon = Icons.Filled.GraphicEq,
            title = "Audio visualizer",
            subtitle = "Show the visualizer control on Now Playing",
            checked = settings.visualizer,
            onChange = viewModel::setVisualizerAvailable
        )
        SettingToggle(
            icon = Icons.Filled.Speed,
            title = "Audio effects",
            subtitle = "Show the Slowed + Reverb effects control",
            checked = settings.audioEffects,
            onChange = viewModel::setAudioEffects
        )
        SettingToggle(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "Volume normalization",
            subtitle = "Even out loudness so quiet tracks aren't too soft",
            checked = settings.volumeNormalization,
            onChange = viewModel::setVolumeNormalization
        )

        SettingToggle(
            icon = Icons.Filled.HighQuality,
            title = "Show quality badges",
            subtitle = "Display Lossless / 320 kbps tags on songs",
            checked = settings.showQualityBadges,
            onChange = viewModel::setShowQualityBadges
        )

        SectionLabel("Hearing safety")
        SettingToggle(
            icon = Icons.Filled.Warning,
            title = "High-volume warning",
            subtitle = "Warn when output reaches a potentially harmful level",
            checked = settings.highVolumeWarning,
            onChange = viewModel::setHighVolumeWarning
        )
        SettingToggle(
            icon = Icons.Filled.Timer,
            title = "Listening-time reminder",
            subtitle = "Suggest a break after an hour of continuous play",
            checked = settings.listeningTimeReminder,
            onChange = viewModel::setListeningTimeReminder
        )
        SettingToggle(
            icon = Icons.Filled.Hearing,
            title = "Max-volume cap",
            subtitle = "Limit output to a safer ceiling",
            checked = settings.maxVolumeCap,
            onChange = viewModel::setMaxVolumeCap
        )
        if (settings.maxVolumeCap) {
            Column(modifier = Modifier.padding(start = 68.dp, end = 20.dp, bottom = 8.dp)) {
                Text(
                    "Ceiling: ${settings.maxVolumePercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.maxVolumePercent.toFloat(),
                    onValueChange = { viewModel.setMaxVolumePercent(it.toInt()) },
                    valueRange = 20f..100f
                )
            }
        }

        SectionLabel("Interaction")
        SettingToggle(
            icon = Icons.Filled.Swipe,
            title = "Swipe gestures on lists",
            subtitle = "Swipe a song to queue it or play it next",
            checked = settings.listSwipeGestures,
            onChange = viewModel::setListSwipeGestures
        )
        SettingToggle(
            icon = Icons.Filled.Notifications,
            title = "In-app notifications",
            subtitle = "Brief confirmations when you like, queue, or add songs",
            checked = settings.inAppToasts,
            onChange = viewModel::setInAppToasts
        )

        SectionLabel("Library")
        GuideRow(
            icon = Icons.Filled.AutoAwesome,
            title = "Group songs by genre",
            subtitle = if (genreBuilding) "Finding genres… ${(genreProgress * 100).toInt()}%"
            else "Scan your library and build a playlist per genre",
            onClick = { if (!genreBuilding) viewModel.buildGenrePlaylists() }
        )
        if (genreBuilding) {
            LinearProgressIndicator(
                progress = { genreProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        SectionLabel("Updates")
        val update = updateState
        val statusLine = when (update) {
            is UpdateState.Checking -> "Checking…"
            is UpdateState.UpToDate -> "You're on the latest version"
            is UpdateState.Available -> "Update ${update.release.tag} available"
            is UpdateState.Downloading -> "Downloading… check your notifications"
            is UpdateState.Error -> update.message
            else -> "Tap to check for updates"
        }
        GuideRow(
            icon = Icons.Filled.SystemUpdate,
            title = "Check for updates",
            subtitle = "v$version • $statusLine",
            onClick = { viewModel.checkForUpdate() }
        )
        if (update is UpdateState.Available) {
            Button(
                onClick = { viewModel.downloadUpdate() },
                modifier = Modifier.padding(start = 68.dp, top = 2.dp, bottom = 4.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Download & install ${update.release.tag}")
            }
        }
        GuideRow(
            icon = Icons.Filled.NewReleases,
            title = "What's new",
            subtitle = "Patch notes for every release",
            onClick = onOpenPatchNotes
        )

        SectionLabel("Guide")
        GuideRow(
            icon = Icons.Filled.TipsAndUpdates,
            title = "Guided tour",
            subtitle = "Spotlight the key features in the app",
            onClick = onReplayTour
        )
        GuideRow(
            icon = Icons.Filled.Replay,
            title = "Welcome carousel",
            subtitle = "Replay the first-launch intro slides",
            onClick = {
                viewModel.setOnboardingSeen(false)
                onBack()
            }
        )

        SectionLabel("About")
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Kaspotify", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A fast, offline player for the music already on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GuideRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(GlassFill)
                .border(1.dp, GlassStroke, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("e.g. Kasra") }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(GlassFill)
                .border(1.dp, GlassStroke, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
