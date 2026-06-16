package com.example.kaspotify.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up a song's genre online by artist + title using Apple's free, key-less iTunes Search API
 * (returns a `primaryGenreName`). Used only as a fallback for tracks whose files carry no embedded
 * genre tag. Network failures return null so the caller can fall back to "Unknown".
 */
@Singleton
class GenreClassifier @Inject constructor() {

    suspend fun lookupGenre(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val term = URLEncoder.encode("$artist $title".trim(), "UTF-8")
            val url = "https://itunes.apple.com/search?term=$term&entity=song&limit=1"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Kaspotify-App")
            }
            val body = try {
                if (conn.responseCode !in 200..299) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val results = JSONObject(body).optJSONArray("results") ?: return@runCatching null
            if (results.length() == 0) return@runCatching null
            results.getJSONObject(0).optString("primaryGenreName").trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
