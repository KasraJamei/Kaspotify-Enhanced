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
    val filePath: String = "",
    /** Plays within the current rolling 7-day window; surfaced in the Most Played section. */
    val weeklyPlayCount: Int = 0
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

    /** Bucketed quality tier used by the "By Quality" browse section. */
    val qualityTier: QualityTier
        get() {
            if (isLossless) return QualityTier.LOSSLESS
            val kbps = bitrateBps?.let { it / 1000 } ?: return QualityTier.UNKNOWN
            return when {
                kbps >= 300 -> QualityTier.KBPS_320
                kbps >= 224 -> QualityTier.KBPS_256
                kbps >= 160 -> QualityTier.KBPS_192
                else -> QualityTier.KBPS_128
            }
        }

    companion object {
        private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
        private val LOSSLESS_MIME_TYPES = listOf("flac", "wav", "x-wav", "alac", "ape")
    }
}

/** Audio-quality buckets, ordered best-first via [ordinal]. */
enum class QualityTier(val label: String) {
    LOSSLESS("Lossless"),
    KBPS_320("320 kbps"),
    KBPS_256("256 kbps"),
    KBPS_192("192 kbps"),
    KBPS_128("≤ 128 kbps"),
    UNKNOWN("Unknown")
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
    val songCount: Int,
    /** Artwork of the first few songs, used to render a 2×2 cover grid. */
    val coverUris: List<Uri> = emptyList()
)
