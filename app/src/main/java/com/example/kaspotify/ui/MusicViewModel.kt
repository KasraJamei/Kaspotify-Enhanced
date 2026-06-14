package com.example.kaspotify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Playlist
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.data.repository.MusicRepository
import com.example.kaspotify.playback.EqualizerController
import com.example.kaspotify.playback.PlayerController
import com.example.kaspotify.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    val player: PlayerController,
    val equalizer: EqualizerController
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
    val playlists: StateFlow<List<Playlist>> = repository.playlists.asState(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<Song>> = repository.search(_searchQuery).asState(emptyList())

    // Playback state surfaced from the controller.
    val currentSong: StateFlow<Song?> get() = player.currentSong
    val isPlaying: StateFlow<Boolean> get() = player.isPlaying
    val positionMs: StateFlow<Long> get() = player.positionMs
    val durationMs: StateFlow<Long> get() = player.durationMs
    val shuffle: StateFlow<Boolean> get() = player.shuffle
    val repeatMode: StateFlow<RepeatMode> get() = player.repeatMode
    val sleepTimerMinutes: StateFlow<Int?> get() = player.sleepTimerMinutes

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
    fun playNext(song: Song) = player.playNext(song)
    fun addToQueue(song: Song) = player.addToQueueEnd(song)
    fun setSleepTimer(minutes: Int?) = player.setSleepTimer(minutes)

    fun toggleFavorite(song: Song) = viewModelScope.launch { repository.toggleFavorite(song) }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ---- Playlist actions ----

    fun createPlaylist(name: String) = viewModelScope.launch { repository.createPlaylist(name) }
    fun renamePlaylist(id: Long, name: String) =
        viewModelScope.launch { repository.renamePlaylist(id, name) }
    fun deletePlaylist(id: Long) = viewModelScope.launch { repository.deletePlaylist(id) }
    fun addToPlaylist(playlistId: Long, song: Song) =
        viewModelScope.launch { repository.addToPlaylist(playlistId, song) }
    fun removeFromPlaylist(playlistId: Long, song: Song) =
        viewModelScope.launch { repository.removeFromPlaylist(playlistId, song) }

    private fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}
