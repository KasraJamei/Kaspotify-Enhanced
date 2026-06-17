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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.ui.GenreEntry
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.theme.GlassFill
import com.example.kaspotify.ui.theme.GlassStroke

@Composable
fun GenreScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scan by viewModel.genreScan.collectAsStateWithLifecycle()
    val entries by viewModel.genreEntries.collectAsStateWithLifecycle()
    val summary by viewModel.genreSummary.collectAsStateWithLifecycle()
    val estimate by viewModel.genreEstimateSeconds.collectAsStateWithLifecycle()

    var onlyUnrecognized by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GenreEntry?>(null) }
    var confirmResetAll by remember { mutableStateOf(false) }

    val analyzed = entries.count { it.analyzed }
    val unrecognized = entries.count { it.analyzed && it.genre == null }
    val shown = if (onlyUnrecognized) entries.filter { it.analyzed && it.genre == null } else entries

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Genres", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { confirmResetAll = true }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Reset all genres")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "analyze") {
                AnalyzeCard(
                    scan = scan,
                    analyzed = analyzed,
                    unrecognized = unrecognized,
                    genreCount = summary.size,
                    estimateSeconds = estimate,
                    onAnalyze = { viewModel.analyzeAndBuildGenres() }
                )
            }

            if (summary.isNotEmpty()) {
                item(key = "summary") {
                    Text(
                        "FOUND GENRES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summary, key = { it.first }) { (genre, count) ->
                            val shape = RoundedCornerShape(percent = 50)
                            Row(
                                modifier = Modifier
                                    .clip(shape)
                                    .background(GlassFill)
                                    .border(1.dp, GlassStroke, shape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("$genre · $count", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item(key = "filter") {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !onlyUnrecognized,
                        onClick = { onlyUnrecognized = false },
                        label = { Text("All songs") }
                    )
                    FilterChip(
                        selected = onlyUnrecognized,
                        onClick = { onlyUnrecognized = true },
                        label = { Text("Unrecognized ($unrecognized)") }
                    )
                }
            }

            items(shown, key = { it.song.id }) { entry ->
                GenreRow(entry = entry, onClick = { editing = entry })
            }
        }
    }

    editing?.let { entry ->
        GenreEditDialog(
            entry = entry,
            onSave = { viewModel.setSongGenre(entry.song.id, it); editing = null },
            onReset = { viewModel.resetSongGenre(entry.song.id); editing = null },
            onDismiss = { editing = null }
        )
    }

    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text("Reset all genres?") },
            text = { Text("This clears the app's saved genre for every song. Your files are not changed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllGenres(); confirmResetAll = false }) { Text("Reset all") }
            },
            dismissButton = { TextButton(onClick = { confirmResetAll = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AnalyzeCard(
    scan: com.example.kaspotify.ui.GenreScanState,
    analyzed: Int,
    unrecognized: Int,
    genreCount: Int,
    estimateSeconds: Int,
    onAnalyze: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(10.dp))
            Text("Group by genre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$analyzed analyzed · $unrecognized unrecognized · $genreCount genres",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (scan.running) {
            val progress = if (scan.total > 0) scan.done.toFloat() / scan.total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(
                        "Analyzing ${scan.done} of ${scan.total}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        buildString {
                            append(scan.currentTitle)
                            append(" → ")
                            append(scan.currentGenre ?: "searching…")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            if (estimateSeconds > 0) {
                Text(
                    "Estimated time: ${formatEstimate(estimateSeconds)} (you can keep using the app)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }
            Button(onClick = onAnalyze) {
                Text(if (analyzed == 0) "Analyze & build playlists" else "Update genres & rebuild")
            }
        }
    }
}

@Composable
private fun GenreRow(entry: GenreEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(10.dp))
        val label = when {
            !entry.analyzed -> "—"
            entry.genre == null -> "Unrecognized"
            else -> entry.genre
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (entry.genre != null) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GenreEditDialog(
    entry: GenreEntry,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(entry.genre ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(
                    "Set this song's genre. Saved only inside Kaspotify — your file isn't changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Rock") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private fun formatEstimate(seconds: Int): String {
    if (seconds < 60) return "~${seconds}s"
    val m = seconds / 60
    val s = seconds % 60
    return if (s == 0) "~${m}m" else "~${m}m ${s}s"
}
