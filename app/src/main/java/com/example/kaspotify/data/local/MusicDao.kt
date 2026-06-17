package com.example.kaspotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // ---- Per-song state ----

    @Upsert
    suspend fun upsertState(state: SongStateEntity)

    @Query("SELECT * FROM song_state WHERE songId = :songId")
    suspend fun getState(songId: Long): SongStateEntity?

    @Query("SELECT songId FROM song_state WHERE isFavorite = 1")
    fun favoriteIds(): Flow<List<Long>>

    @Query("SELECT songId FROM song_state WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recentlyPlayedIds(limit: Int = 50): Flow<List<Long>>

    @Query("SELECT songId FROM song_state WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun mostPlayedIds(limit: Int = 50): Flow<List<Long>>

    /** Full state rows for every song that's ever been played, for weekly-aware ranking. */
    @Query("SELECT * FROM song_state WHERE playCount > 0")
    fun playedStates(): Flow<List<SongStateEntity>>

    // ---- Playlists ----

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: Long)

    @Transaction
    suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistSongs(playlistId)
        deletePlaylistRow(playlistId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun playlistSongIds(playlistId: Long): Flow<List<Long>>

    /** All playlist→song links, position-ordered, for building cover grids. */
    @Query("SELECT playlistId, songId FROM playlist_songs ORDER BY position ASC")
    fun allPlaylistSongRefs(): Flow<List<PlaylistSongLite>>

    @Query(
        """
        SELECT p.id AS id, p.name AS name,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlistId = p.id) AS songCount
        FROM playlists p
        ORDER BY p.createdAt DESC
        """
    )
    fun playlistsWithCounts(): Flow<List<PlaylistWithCount>>

    // ---- Genre metadata (the app's own per-song genre store) ----

    @Upsert
    suspend fun upsertGenre(entry: SongGenreEntity)

    @Query("SELECT * FROM song_genre")
    fun genres(): Flow<List<SongGenreEntity>>

    @Query("SELECT * FROM song_genre")
    suspend fun genresOnce(): List<SongGenreEntity>

    @Query("DELETE FROM song_genre WHERE songId = :songId")
    suspend fun deleteGenre(songId: Long)

    @Query("DELETE FROM song_genre")
    suspend fun clearGenres()

    // ---- Search history ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(entry: SearchHistoryEntity)

    @Query("SELECT query FROM search_history ORDER BY lastUsedAt DESC LIMIT :limit")
    fun recentSearches(limit: Int = 12): Flow<List<String>>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
