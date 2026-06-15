package com.example.kaspotify.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.playback.EqMode
import com.example.kaspotify.playback.EqualizerController
import com.example.kaspotify.playback.SimpleBand
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.GradientBackdrop
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassFillStrong
import com.example.kaspotify.ui.theme.GlassStroke
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eq = viewModel.equalizer
    val available by eq.available.collectAsStateWithLifecycle()
    val enabled by eq.enabled.collectAsStateWithLifecycle()
    val mode by eq.mode.collectAsStateWithLifecycle()

    GradientBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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
            ModeToggle(mode = mode, onSelect = { eq.setMode(it) })

            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == EqMode.SIMPLE) "Three quick controls — drag to taste, from -10 to +10."
                else "Pick a preset, or fine-tune every frequency band yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (mode) {
                    EqMode.SIMPLE -> SimpleEqControls(eq)
                    EqMode.ADVANCED -> AdvancedEqControls(eq)
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: EqMode, onSelect: (EqMode) -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EqMode.entries.forEach { m ->
            val selected = m == mode
            val bg by animateColorAsState(
                targetValue = if (selected) GlassFillStrong else Color.Transparent,
                label = "eqSegment"
            )
            val content by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "eqSegmentText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(bg)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (m == EqMode.SIMPLE) "Simple" else "Advanced",
                    style = MaterialTheme.typography.labelLarge,
                    color = content
                )
            }
        }
    }
}

// ---- Simple: Bass / Mid / Treble on a -10..+10 scale ----

@Composable
private fun SimpleEqControls(eq: EqualizerController) {
    val levels by eq.simpleLevels.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SimpleBand.entries.forEach { band ->
            SimpleBandSlider(
                label = band.displayName(),
                hint = band.hint(),
                value = levels[band] ?: 0,
                onValueChange = { eq.setSimpleLevel(band, it) }
            )
        }
        Spacer(Modifier.height(4.dp))
        ResetPill(label = "Reset to flat", onClick = { eq.resetBands() })
    }
}

@Composable
private fun SimpleBandSlider(
    label: String,
    hint: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatSigned(value),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = -EqualizerController.SIMPLE_RANGE.toFloat()..EqualizerController.SIMPLE_RANGE.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = GlassStroke
            )
        )
    }
}

@Composable
private fun ResetPill(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---- Advanced: native presets + per-band sliders ----

@Composable
private fun AdvancedEqControls(eq: EqualizerController) {
    val bands by eq.bands.collectAsStateWithLifecycle()
    val (minLevel, maxLevel) = eq.levelRange.collectAsStateWithLifecycle().value
    val presets by eq.presets.collectAsStateWithLifecycle()
    val currentPreset by eq.currentPreset.collectAsStateWithLifecycle()

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
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = GlassStroke
                ),
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

    Spacer(Modifier.height(20.dp))
    ResetPill(label = "Reset bands", onClick = { eq.resetBands() })
    Spacer(Modifier.height(8.dp))
}

private fun SimpleBand.displayName(): String = when (this) {
    SimpleBand.BASS -> "Bass"
    SimpleBand.MID -> "Mid"
    SimpleBand.TREBLE -> "Treble"
}

private fun SimpleBand.hint(): String = when (this) {
    SimpleBand.BASS -> "Low-end punch & warmth"
    SimpleBand.MID -> "Vocals & instrument body"
    SimpleBand.TREBLE -> "Air, detail & sparkle"
}

private fun formatSigned(value: Int): String = if (value > 0) "+$value" else "$value"

private fun formatFreq(milliHz: Int): String {
    val hz = milliHz / 1000
    return if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
}
