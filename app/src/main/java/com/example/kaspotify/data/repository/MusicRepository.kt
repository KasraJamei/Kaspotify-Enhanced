package com.example.kaspotify.data.repository

import com.example.kaspotify.data.local.MusicDao
import com.example.kaspotify.data.local.PlaylistEntity
import com.example.kaspotify.data.local.PlaylistSongCrossRef
import com.example.kaspotify.data.local.SearchHistoryEntity
import com.example.kaspotify.data.local.SongStateEntity
import com.example.kaspotify.data.media.GenreClassifier
import com.example.kaspotify.data.media.MediaStoreImporter
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Playlist
import com.example.kaspotify.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val importer: MediaStoreImporter,
    private val genreClassifier: GenreClassifier,
    private val dao: MusicDao
) {

    private val scannedSongs = MutableStateFlow<List<Song>>(emptyList())

    /** Raw scanned songs with no Room state merged in. */
    val rawSongs: Flow<List<Song>> = scannedSongs.asStateFlow()

    /** Live set of favorited song ids — used to keep the now-playing favorite state fresh. */
    val favoriteIds: Flow<Set<Long>> = dao.favoriteIds().map { it.toHashSet() }

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

    /**
     * Most Played, prioritized by plays in the current rolling 7-day window (then all-time as a
     * tiebreak/fallback). Each returned song carries its [Song.weeklyPlayCount] for the UI label.
     */
    val mostPlayed: Flow<List<Song>> =
        combine(songs, dao.playedStates()) { list, states ->
            val now = System.currentTimeMillis()
            val byId = list.associateBy { it.id }
            states.mapNotNull { st ->
                val song = byId[st.songId] ?: return@mapNotNull null
                val weekly = if (now - st.weekStartAt < WEEK_MS) st.weeklyPlayCount else 0
                Triple(song, weekly, st.playCount)
            }
                .sortedWith(
                    compareByDescending<Triple<Song, Int, Int>> { it.second }
                        .thenByDescending { it.third }
                )
                .map { (song, weekly, _) -> song.copy(weeklyPlayCount = weekly) }
                .take(MAX_SMART_PLAYLIST_SIZE)
        }

    val recentlyAdded: Flow<List<Song>> =
        songs.map { list -> list.sortedByDescending { it.dateAddedSec }.take(MAX_SMART_PLAYLIST_SIZE) }

    val playlists: Flow<List<Playlist>> =
        dao.playlistsWithCounts().map { rows ->
            rows.map { Playlist(id = it.id, name = it.name, songCount = it.songCount) }
        }

    suspend fun refreshLibrary() {
        scannedSongs.value = importer.scan()
    }

    // Serializes favorite writes so rapid taps (e.g. double-tap like/unlike) can't race a
    // read-modify-write and land on a non-deterministic state.
    private val favoriteMutex = Mutex()

    suspend fun toggleFavorite(song: Song) = favoriteMutex.withLock {
        val current = dao.getState(song.id)
        val updated = (current ?: SongStateEntity(songId = song.id))
            .copy(isFavorite = !(current?.isFavorite ?: false))
        dao.upsertState(updated)
    }

    /**
     * Sets an explicit favorite state (idempotent). Preferred for double-tap, which needs a
     * deterministic result under fast taps rather than a toggle that can race itself.
     */
    suspend fun setFavorite(songId: Long, favorite: Boolean) = favoriteMutex.withLock {
        val current = dao.getState(songId) ?: SongStateEntity(songId = songId)
        if (current.isFavorite != favorite) dao.upsertState(current.copy(isFavorite = favorite))
    }

    suspend fun recordPlay(song: Song) {
        val now = System.currentTimeMillis()
        val current = dao.getState(song.id) ?: SongStateEntity(songId = song.id)
        // Roll the weekly window: if it's been a week (or never started), reset to 1, else increment.
        val newWindow = current.weekStartAt == 0L || now - current.weekStartAt >= WEEK_MS
        dao.upsertState(
            current.copy(
                playCount = current.playCount + 1,
                lastPlayedAt = now,
                weeklyPlayCount = if (newWindow) 1 else current.weeklyPlayCount + 1,
                weekStartAt = if (newWindow) now else current.weekStartAt
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

    // ---- Search history ----

    val recentSearches: Flow<List<String>> = dao.recentSearches()

    suspend fun recordSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        dao.upsertSearch(SearchHistoryEntity(query = q))
    }

    suspend fun removeSearch(query: String) = dao.deleteSearch(query)

    suspend fun clearSearchHistory() = dao.clearSearchHistory()

    // ---- Bulk playlist creation (used by the smart-playlist builder) ----

    suspend fun createPlaylistWith(name: String, songs: List<Song>): Long {
        val id = createPlaylist(name)
        songs.forEachIndexed { index, song ->
            dao.insertPlaylistSong(
                PlaylistSongCrossRef(playlistId = id, songId = song.id, position = index)
            )
        }
        return id
    }

    // ---- Genre auto-playlists ("AI" genre grouping) ----

    /**
     * Scans the library, resolves each song's genre (embedded MediaStore tag first, then an online
     * lookup by name for untagged tracks, capped to keep it quick), groups them, and (re)creates a
     * smart playlist per genre with enough songs. [onProgress] reports (done, total) over the slow
     * online-lookup phase. Returns the number of genre playlists created.
     */
    suspend fun buildGenrePlaylists(onProgress: (Int, Int) -> Unit): Int {
        val library = scannedSongs.value
        if (library.isEmpty()) return 0

        val genreOf = HashMap<Long, String>()
        importer.scanGenres().forEach { (id, g) -> genreOf[id] = normalizeGenre(g) }

        // Untagged songs → online lookup, capped so a huge library doesn't hammer the network.
        val untagged = library.filter { it.id !in genreOf }.take(MAX_ONLINE_LOOKUPS)
        untagged.forEachIndexed { index, song ->
            onProgress(index, untagged.size)
            genreClassifier.lookupGenre(song.artist, song.title)?.let {
                genreOf[song.id] = normalizeGenre(it)
            }
            delay(120) // be polite to the lookup service
        }
        onProgress(untagged.size, untagged.size)

        val byGenre = library.groupBy { genreOf[it.id] }
            .filterKeys { !it.isNullOrEmpty() }
            .mapKeys { it.key!! }
            .filterValues { it.size >= MIN_SONGS_PER_GENRE }
        if (byGenre.isEmpty()) return 0

        // Replace any previous genre playlists of the same name so re-running doesn't duplicate.
        val existing = dao.playlistsWithCounts().first().associate { it.name to it.id }
        var created = 0
        for ((genre, genreSongs) in byGenre) {
            val name = "$GENRE_PREFIX$genre"
            existing[name]?.let { dao.deletePlaylist(it) }
            createPlaylistWith(name, genreSongs.take(MAX_SMART_PLAYLIST_SIZE))
            created++
        }
        return created
    }

    private fun normalizeGenre(raw: String): String =
        raw.trim().split(Regex("[/;,]")).first().trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun orderByIds(songs: List<Song>, ids: List<Long>): List<Song> {
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    companion object {
        private const val MAX_SMART_PLAYLIST_SIZE = 100
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        private const val MAX_ONLINE_LOOKUPS = 60
        private const val MIN_SONGS_PER_GENRE = 3
        const val GENRE_PREFIX = "Genre · "
    }
}
