package com.example.kaspotify.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.playback.ReverbPreset
import com.example.kaspotify.ui.MusicViewModel
import java.util.Locale

@Composable
fun EffectsScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reverb = viewModel.reverb
    val reverbAvailable by reverb.available.collectAsStateWithLifecycle()
    val reverbEnabled by reverb.enabled.collectAsStateWithLifecycle()
    val reverbPreset by reverb.preset.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

    val slowedReverbOn = reverbEnabled && reverbPreset == ReverbPreset.LARGE_HALL && speed < 1f

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
                }
                Text("Effects", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Slowed + Reverb", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "0.85x speed with a large hall reverb",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = slowedReverbOn,
                        onCheckedChange = { viewModel.setSlowedReverbEnabled(it) },
                        enabled = reverbAvailable
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Playback speed", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = speed,
                    valueRange = 0.5f..1.25f,
                    onValueChange = { viewModel.setPlaybackSpeed(it) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    String.format(Locale.US, "%.2fx", speed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).width(52.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reverb", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = reverbEnabled,
                    onCheckedChange = { reverb.setEnabled(it) },
                    enabled = reverbAvailable
                )
            }

            if (!reverbAvailable) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Reverb is not available on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReverbPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = reverbPreset == preset,
                        onClick = { reverb.setPreset(preset) },
                        label = { Text(preset.label) },
                        enabled = reverbEnabled
                    )
                }
            }
        }
    }
}
