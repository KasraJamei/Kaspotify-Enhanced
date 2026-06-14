package com.example.kaspotify.data.repository

import com.example.kaspotify.data.local.MusicDao
import com.example.kaspotify.data.local.PlaylistEntity
import com.example.kaspotify.data.local.PlaylistSongCrossRef
import com.example.kaspotify.data.local.SongStateEntity
import com.example.kaspotify.data.media.MediaStoreImporter
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Playlist
import com.example.kaspotify.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val importer: MediaStoreImporter,
    private val dao: MusicDao
) {

    private val scannedSongs = MutableStateFlow<List<Song>>(emptyList())

    /** Raw scanned songs with no Room state merged in. */
    val rawSongs: Flow<List<Song>> = scannedSongs.asStateFlow()

    /** Songs with favorite state merged from Room. */
    val songs: Flow<List<Song>> =
        combine(scannedSongs, dao.favoriteIds()) { list, favoriteIds ->
            val favorites = favoriteIds.toHashSet()
            list.map { it.copy(isFavorite = it.id in favorites) }
        }

    val albums: Flow<List<Album>> = songs.map { importer.deriveAlbums(it) }

    val artists: Flow<List<Artist>> = songs.map { importer.deriveArtists(it) }

    val favorites: Flow<List<Song>> = songs.map { list -> list.filter { it.isFavorite } }

    val recentlyPlayed: Flow<List<Song>> =
        combine(songs, dao.recentlyPlayedIds()) { list, ids -> orderByIds(list, ids) }

    val mostPlayed: Flow<List<Song>> =
        combine(songs, dao.mostPlayedIds()) { list, ids -> orderByIds(list, ids) }

    val recentlyAdded: Flow<List<Song>> =
        songs.map { list -> list.sortedByDescending { it.dateAddedSec }.take(MAX_SMART_PLAYLIST_SIZE) }

    val playlists: Flow<List<Playlist>> =
        dao.playlistsWithCounts().map { rows ->
            rows.map { Playlist(id = it.id, name = it.name, songCount = it.songCount) }
        }

    suspend fun refreshLibrary() {
        scannedSongs.value = importer.scan()
    }

    suspend fun toggleFavorite(song: Song) {
        val current = dao.getState(song.id)
        val updated = (current ?: SongStateEntity(songId = song.id))
            .copy(isFavorite = !(current?.isFavorite ?: false))
        dao.upsertState(updated)
    }

    suspend fun recordPlay(song: Song) {
        val current = dao.getState(song.id) ?: SongStateEntity(songId = song.id)
        dao.upsertState(
            current.copy(
                playCount = current.playCount + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    // ---- Playlists ----

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name.trim().ifEmpty { "New playlist" }))

    suspend fun renamePlaylist(playlistId: Long, name: String) =
        dao.renamePlaylist(playlistId, name.trim())

    suspend fun deletePlaylist(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun addToPlaylist(playlistId: Long, song: Song) {
        val position = dao.nextPosition(playlistId)
        dao.insertPlaylistSong(
            PlaylistSongCrossRef(playlistId = playlistId, songId = song.id, position = position)
        )
    }

    suspend fun removeFromPlaylist(playlistId: Long, song: Song) =
        dao.removeSongFromPlaylist(playlistId, song.id)

    fun playlistSongs(playlistId: Long): Flow<List<Song>> =
        combine(songs, dao.playlistSongIds(playlistId)) { list, ids -> orderByIds(list, ids) }

    // ---- Search ----

    fun search(queryFlow: Flow<String>): Flow<List<Song>> =
        combine(songs, queryFlow) { list, query ->
            val q = query.trim()
            if (q.isEmpty()) {
                emptyList()
            } else {
                list.filter { song ->
                    song.title.contains(q, ignoreCase = true) ||
                        song.artist.contains(q, ignoreCase = true) ||
                        song.album.contains(q, ignoreCase = true)
                }
            }
        }

    private fun orderByIds(songs: List<Song>, ids: List<Long>): List<Song> {
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    companion object {
        private const val MAX_SMART_PLAYLIST_SIZE = 100
    }
}
