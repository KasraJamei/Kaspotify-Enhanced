package com.example.kaspotify.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-song persisted state. songId matches MediaStore audio _ID. */
@Entity(tableName = "song_state")
data class SongStateEntity(
    @PrimaryKey val songId: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    /** Plays within the current rolling 7-day window (resets weekly). */
    val weeklyPlayCount: Int = 0,
    /** Epoch millis when the current weekly window started. */
    val weekStartAt: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

/** Projection used by playlistsWithCounts(). */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val songCount: Int
)

/** A past search term, kept for the search history list. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)

/**
 * The app's own genre metadata for a song, kept separate from the file's tags (we never write to
 * the audio file). [genre] null means the song was analyzed but no genre could be found. [source]
 * is one of "tag" / "online" / "manual".
 */
@Entity(tableName = "song_genre")
data class SongGenreEntity(
    @PrimaryKey val songId: Long,
    val genre: String?,
    val source: String,
    val checkedAt: Long = System.currentTimeMillis()
)
