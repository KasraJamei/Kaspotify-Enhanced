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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.kaspotify.ui.MusicViewModel

@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eq = viewModel.equalizer
    val available by eq.available.collectAsStateWithLifecycle()
    val enabled by eq.enabled.collectAsStateWithLifecycle()
    val bands by eq.bands.collectAsStateWithLifecycle()
    val (minLevel, maxLevel) = eq.levelRange.collectAsStateWithLifecycle().value
    val presets by eq.presets.collectAsStateWithLifecycle()
    val currentPreset by eq.currentPreset.collectAsStateWithLifecycle()

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
                Text("Equalizer", style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = enabled,
                    onCheckedChange = { eq.setEnabled(it) },
                    enabled = available
                )
            }

            if (!available) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Equalizer is not available on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            Spacer(Modifier.height(16.dp))
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, name ->
                    FilterChip(
                        selected = currentPreset == index,
                        onClick = { eq.usePreset(index) },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Bands", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val range = (maxLevel - minLevel).coerceAtLeast(1)
            bands.forEach { band ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            formatFreq(band.centerFreqMilliHz),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = (band.level - minLevel).toFloat() / range,
                        onValueChange = { fraction ->
                            val level = (minLevel + fraction * range).toInt().toShort()
                            eq.setBandLevel(band.index, level)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${band.level / 100} dB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private fun formatFreq(milliHz: Int): String {
    val hz = milliHz / 1000
    return if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
}
