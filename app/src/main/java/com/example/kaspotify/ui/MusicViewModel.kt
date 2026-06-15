package com.example.kaspotify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Playlist
import com.example.kaspotify.data.model.QualityTier
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.data.repository.MusicRepository
import com.example.kaspotify.playback.EqualizerController
import com.example.kaspotify.playback.PlayerController
import com.example.kaspotify.playback.RepeatMode
import com.example.kaspotify.playback.ReverbController
import com.example.kaspotify.playback.ReverbPreset
import com.example.kaspotify.playback.VisualizerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    val player: PlayerController,
    val equalizer: EqualizerController,
    val visualizer: VisualizerController,
    val reverb: ReverbController
) : ViewModel() {

    fun songsForAlbum(albumId: Long): List<Song> =
        songs.value.filter { it.albumId == albumId }

    fun songsForArtist(artistName: String): List<Song> =
        songs.value.filter { it.artist == artistName }

    val songs: StateFlow<List<Song>> = repository.songs.asState(emptyList())
    val albums: StateFlow<List<Album>> = repository.albums.asState(emptyList())
    val artists: StateFlow<List<Artist>> = repository.artists.asState(emptyList())
    val favorites: StateFlow<List<Song>> = repository.favorites.asState(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayed.asState(emptyList())
    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayed.asState(emptyList())
    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAdded.asState(emptyList())
    val playlists: StateFlow<List<Playlist>> = repository.playlists.asState(emptyList())

    /** One-shot, user-facing action confirmations shown as a transient in-app snackbar. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private fun notify(message: String) { _messages.tryEmit(message) }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<Song>> = repository.search(_searchQuery).asState(emptyList())
    val recentSearches: StateFlow<List<String>> = repository.recentSearches.asState(emptyList())

    /** A daily-rotating mix, deterministic per calendar day. */
    val playlistOfTheDay: StateFlow<List<Song>> =
        repository.songs.map { dailyPick(it) }.asState(emptyList())

    /** All songs grouped into quality tiers (best tier first) for the "By Quality" browser. */
    val songsByQuality: StateFlow<Map<QualityTier, List<Song>>> =
        repository.songs
            .map { list -> list.groupBy { it.qualityTier }.toSortedMap(compareBy { it.ordinal }) }
            .asState(emptyMap())

    // Playback state surfaced from the controller.
    /**
     * The playing track, with its favorite flag kept live from Room. The PlayerController captures a
     * song's `isFavorite` when it's enqueued, so liking/unliking from Now Playing wouldn't otherwise
     * recolor the heart — we merge the current favorite set in here so the UI stays correct.
     */
    val currentSong: StateFlow<Song?> =
        combine(player.currentSong, repository.favoriteIds) { song, favIds ->
            song?.copy(isFavorite = song.id in favIds)
        }.asState(null)
    val isPlaying: StateFlow<Boolean> get() = player.isPlaying
    val positionMs: StateFlow<Long> get() = player.positionMs
    val durationMs: StateFlow<Long> get() = player.durationMs
    val shuffle: StateFlow<Boolean> get() = player.shuffle
    val repeatMode: StateFlow<RepeatMode> get() = player.repeatMode
    val sleepTimerMinutes: StateFlow<Int?> get() = player.sleepTimerMinutes
    val playbackSpeed: StateFlow<Float> get() = player.playbackSpeed
    val queue: StateFlow<List<Song>> get() = player.queue
    val queueIndex: StateFlow<Int> get() = player.queueIndex

    init {
        // Count a play whenever a new track starts.
        viewModelScope.launch {
            player.currentSong
                .distinctUntilChangedBy { it?.id }
                .collect { song -> song?.let { repository.recordPlay(it) } }
        }
    }

    fun refreshLibrary() = viewModelScope.launch { repository.refreshLibrary() }

    fun playlistSongs(playlistId: Long): Flow<List<Song>> = repository.playlistSongs(playlistId)

    // ---- Playback actions ----

    fun play(queue: List<Song>, index: Int) = player.playQueue(queue, index)

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.playQueue(queue, index)
    }

    fun shuffleAll(queue: List<Song>) {
        if (queue.isEmpty()) return
        if (!player.shuffle.value) player.toggleShuffle()
        player.playQueue(queue.shuffled(), 0)
    }

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun playNext(song: Song) {
        player.playNext(song)
        notify("Playing next: ${song.title}")
    }

    fun addToQueue(song: Song) {
        player.addToQueueEnd(song)
        notify("Added to queue: ${song.title}")
    }

    fun setSleepTimer(minutes: Int?) {
        player.setSleepTimer(minutes)
        notify(if (minutes != null) "Sleep timer set for $minutes min" else "Sleep timer off")
    }
    fun moveQueueItem(from: Int, to: Int) = player.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = player.removeQueueItem(index)
    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    // ---- Visualizer / Reverb ----

    fun setVisualizerEnabled(enabled: Boolean) = visualizer.setEnabled(enabled)
    fun setReverbEnabled(enabled: Boolean) = reverb.setEnabled(enabled)
    fun setReverbPreset(preset: ReverbPreset) = reverb.setPreset(preset)

    /** One-tap "Slowed + Reverb" mode: ~0.85x speed with a large hall reverb. */
    fun setSlowedReverbEnabled(enabled: Boolean) {
        if (enabled) {
            player.setPlaybackSpeed(0.85f)
            reverb.setPreset(ReverbPreset.LARGE_HALL)
            reverb.setEnabled(true)
        } else {
            player.setPlaybackSpeed(1f)
            reverb.setEnabled(false)
        }
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        repository.toggleFavorite(song)
        notify(if (song.isFavorite) "Removed from Liked" else "Added to Liked")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun recordSearch(query: String) = viewModelScope.launch { repository.recordSearch(query) }
    fun removeSearch(query: String) = viewModelScope.launch { repository.removeSearch(query) }
    fun clearSearchHistory() = viewModelScope.launch { repository.clearSearchHistory() }

    /** Creates and saves a playlist populated with [songs] (used by the smart-playlist builder). */
    fun createSmartPlaylist(name: String, songs: List<Song>) =
        viewModelScope.launch {
            repository.createPlaylistWith(name, songs)
            notify("Created \"$name\" with ${songs.size} songs")
        }

    private fun dailyPick(all: List<Song>): List<Song> {
        if (all.isEmpty()) return emptyList()
        val epochDay = System.currentTimeMillis() / 86_400_000L
        return all.shuffled(Random(epochDay)).take(DAILY_PLAYLIST_SIZE)
    }

    // ---- Playlist actions ----

    fun createPlaylist(name: String) = viewModelScope.launch {
        repository.createPlaylist(name)
        notify("Playlist created")
    }
    fun renamePlaylist(id: Long, name: String) =
        viewModelScope.launch { repository.renamePlaylist(id, name) }
    fun deletePlaylist(id: Long) = viewModelScope.launch {
        repository.deletePlaylist(id)
        notify("Playlist deleted")
    }
    fun addToPlaylist(playlistId: Long, song: Song) =
        viewModelScope.launch {
            repository.addToPlaylist(playlistId, song)
            notify("Added to playlist")
        }
    fun removeFromPlaylist(playlistId: Long, song: Song) =
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, song)
            notify("Removed from playlist")
        }

    private fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private companion object {
        const val DAILY_PLAYLIST_SIZE = 30
    }
}
