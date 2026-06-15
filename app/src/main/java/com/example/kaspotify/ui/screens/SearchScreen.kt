package com.example.kaspotify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.ui.MusicViewModel
import com.example.kaspotify.ui.components.SongRow

private enum class SearchMode { RECENT, EMPTY, RESULTS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentId = currentSong?.id
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Search",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = scaleIn(tween(150)) + fadeIn(tween(150)),
                    exit = scaleOut(tween(150)) + fadeOut(tween(150))
                ) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.kaspotify.ui.theme.GlassStroke,
                unfocusedBorderColor = com.example.kaspotify.ui.theme.GlassStroke,
                focusedContainerColor = com.example.kaspotify.ui.theme.GlassFill,
                unfocusedContainerColor = com.example.kaspotify.ui.theme.GlassFill
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.recordSearch(query)
                keyboard?.hide()
            })
        )

        // Cross-fade between the three search states (recent history / no results / results) so
        // the content gently transitions as you type instead of snapping.
        val mode = when {
            query.isBlank() -> SearchMode.RECENT
            results.isEmpty() -> SearchMode.EMPTY
            else -> SearchMode.RESULTS
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "searchMode"
        ) { m ->
            when (m) {
                SearchMode.RECENT -> RecentSearches(
                    recentSearches = recentSearches,
                    onPick = { viewModel.onSearchQueryChange(it) },
                    onRemove = { viewModel.removeSearch(it) },
                    onClear = { viewModel.clearSearchHistory() }
                )
                SearchMode.EMPTY -> CenterText("No results for \"$query\"")
                SearchMode.RESULTS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentId,
                            onClick = {
                                viewModel.recordSearch(query)
                                viewModel.playSong(song, results)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onMore = { onMore(song) },
                            onPlayNext = { viewModel.playNext(song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            // Rows slide to their new position as the query narrows the list.
                            modifier = Modifier.animateItemPlacement(tween(250))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSearches(
    recentSearches: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    if (recentSearches.isEmpty()) {
        CenterText("Find your music")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recent searches", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(recentSearches, key = { it }) { term ->
                Row(
                    modifier = Modifier
                        .animateItemPlacement(tween(250))
                        .fillMaxWidth()
                        .clickable { onPick(term) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            term,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    IconButton(onClick = { onRemove(term) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
