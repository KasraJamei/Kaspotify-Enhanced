package com.example.kaspotify.data.model

import android.content.ContentUris
import android.net.Uri

/** A single playable track imported from the device. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val track: Int,
    val year: Int,
    val dateAddedSec: Long,
    val isFavorite: Boolean = false,
    val mimeType: String = "",
    val bitrateBps: Int? = null,
    val filePath: String = ""
) {
    /** Album-art content URI derived from the album id. May not resolve for every album. */
    val artworkUri: Uri
        get() = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)

    /** Whether the source format is lossless (FLAC, WAV, ALAC, etc.). */
    val isLossless: Boolean
        get() = LOSSLESS_MIME_TYPES.any { mimeType.contains(it, ignoreCase = true) }

    /** Short human-readable quality label, e.g. "Lossless", "320 kbps", "AAC". */
    val qualityLabel: String
        get() {
            if (isLossless) return "Lossless"
            val kbps = bitrateBps?.let { it / 1000 }
            val format = when {
                mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
                mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "AAC"
                mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                mimeType.contains("opus", ignoreCase = true) -> "Opus"
                else -> null
            }
            return when {
                kbps != null && format != null -> "$format $kbps kbps"
                kbps != null -> "$kbps kbps"
                format != null -> format
                else -> ""
            }
        }

    companion object {
        private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
        private val LOSSLESS_MIME_TYPES = listOf("flac", "wav", "x-wav", "alac", "ape")
    }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int
) {
    val artworkUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
}

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int
)
