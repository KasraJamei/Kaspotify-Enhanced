package com.example.kaspotify

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.ui.AppScaffold
import com.example.kaspotify.ui.LocalAppSettings
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.theme.KaspotifyTheme
import com.example.kaspotify.ui.theme.asAmbient
import com.example.kaspotify.ui.theme.rememberArtworkAccentColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
            val artworkAccent by rememberArtworkAccentColor(currentSong?.artworkUri)

            // Apply the high-refresh-rate hint whenever the toggle changes.
            LaunchedEffect(settings.highRefreshRate) { applyHighestRefreshRate(settings.highRefreshRate) }

            // The accent stays platinum; the art color only feeds the ambient gradient backdrop,
            // and only when album-art theming is enabled.
            val ambientColor = if (settings.albumArtTheming) artworkAccent?.asAmbient() else null
            CompositionLocalProvider(LocalAppSettings provides settings) {
                KaspotifyTheme(ambientColor = ambientColor) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PermissionGate(viewModel)
                    }
                }
            }
        }
    }

    /**
     * Ask the display for its highest refresh-rate mode so animations/scrolling run as smooth as the
     * panel allows. When [enabled] is false, clears the hint back to the system default.
     */
    private fun applyHighestRefreshRate(enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val modeId = if (enabled) {
                    display?.supportedModes?.maxByOrNull { it.refreshRate }?.modeId ?: 0
                } else 0
                window.attributes = window.attributes.apply { preferredDisplayModeId = modeId }
            } else {
                @Suppress("DEPRECATION")
                val best = if (enabled) {
                    windowManager.defaultDisplay.supportedRefreshRates.maxOrNull() ?: 0f
                } else 0f
                window.attributes = window.attributes.apply {
                    @Suppress("DEPRECATION")
                    preferredRefreshRate = best
                }
            }
        } catch (_: Throwable) {
            // Refresh-rate hints are best-effort; ignore unsupported devices.
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGate(viewModel: MusicViewModel) {
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.READ_MEDIA_AUDIO)
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    // The audio read permission is the first entry and is what gates the library.
    val audioGranted = permissionState.permissions.first().status.isGranted

    if (audioGranted) {
        LaunchedEffect(Unit) {
            viewModel.player.connect()
            viewModel.refreshLibrary()
        }
        AppScaffold(viewModel)
    } else {
        Rationale(onGrant = { permissionState.launchMultiplePermissionRequest() })
    }
}

@Composable
private fun Rationale(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            "Kaspotify",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "Kaspotify plays the music already on your device. " +
                "Grant access to your audio files to import your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onGrant) {
            Text("Grant access & import")
        }
    }
}
