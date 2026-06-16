package com.example.kaspotify.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.kaspotify.data.model.Album
import com.example.kaspotify.data.model.Artist
import com.example.kaspotify.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads music already present on the device through MediaStore. No network, no streaming. */
@Singleton
class MediaStoreImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Scan device audio for actual music tracks (skips ringtones / very short clips). */
    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.BITRATE)
            }
        }.toTypedArray()
        // Music only, and at least 5 seconds long.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = ArrayList<Song>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown title",
                        artist = cursor.getString(artistCol)?.takeUnless { it == "<unknown>" }
                            ?: "Unknown artist",
                        album = cursor.getString(albumCol) ?: "Unknown album",
                        albumId = cursor.getLong(albumIdCol),
                        durationMs = cursor.getLong(durationCol),
                        uri = uri,
                        track = cursor.getInt(trackCol),
                        year = cursor.getInt(yearCol),
                        dateAddedSec = cursor.getLong(dateCol),
                        mimeType = cursor.getString(mimeCol) ?: "",
                        bitrateBps = if (bitrateCol >= 0) {
                            cursor.getInt(bitrateCol).takeIf { it > 0 }
                        } else null,
                        filePath = cursor.getString(dataCol) ?: ""
                    )
                }
            }
        songs
    }

    /**
     * Reads genre tags already embedded in the library via MediaStore's genre tables, returning a
     * songId → genre-name map. Songs whose files carry no genre tag simply won't appear here.
     */
    suspend fun scanGenres(): Map<Long, String> = withContext(Dispatchers.IO) {
        val result = HashMap<Long, String>()
        val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            genresUri,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { gc ->
            val idCol = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (gc.moveToNext()) {
                val genreId = gc.getLong(idCol)
                val name = gc.getString(nameCol)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                context.contentResolver.query(
                    membersUri,
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null, null, null
                )?.use { mc ->
                    val audioCol = mc.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    while (mc.moveToNext()) {
                        result[mc.getLong(audioCol)] = name
                    }
                }
            }
        }
        result
    }

    fun deriveAlbums(songs: List<Song>): List<Album> =
        songs.groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = albumSongs.map { it.artist }.distinct().singleOrNull()
                        ?: "Various artists",
                    songCount = albumSongs.size
                )
            }
            .sortedBy { it.title.lowercase() }

    fun deriveArtists(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist }
            .map { (name, artistSongs) ->
                Artist(
                    name = name,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.map { it.albumId }.distinct().size
                )
            }
            .sortedBy { it.name.lowercase() }

    companion object {
        private const val MIN_DURATION_MS = 5_000L
    }
}
