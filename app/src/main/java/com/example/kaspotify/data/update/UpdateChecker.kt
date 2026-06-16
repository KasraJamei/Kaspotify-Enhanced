package com.example.kaspotify.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** A single GitHub release, parsed into what the UI needs. */
data class ReleaseInfo(
    val version: String,      // tag without a leading "v", e.g. "1.8"
    val tag: String,          // raw tag, e.g. "v1.8"
    val title: String,
    val notes: String,
    val publishedAt: String,  // ISO date (YYYY-MM-DD shown)
    val apkUrl: String?
)

/**
 * Talks to the GitHub Releases API to power the in-app patch-notes list and a non-forced update
 * check, and can download + launch the installer for a newer release APK. Plain HttpURLConnection +
 * org.json so no networking dependency is added.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun currentVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "0"

    /** All releases, newest first. Throws on network/parse failure (caller maps to an error state). */
    suspend fun fetchReleases(): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        val json = httpGet("https://api.github.com/repos/$REPO/releases?per_page=30")
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optBoolean("draft", false)) continue
                val tag = o.optString("tag_name")
                val assets = o.optJSONArray("assets")
                var apk: String? = null
                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val name = assets.getJSONObject(j).optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apk = assets.getJSONObject(j).optString("browser_download_url")
                            break
                        }
                    }
                }
                add(
                    ReleaseInfo(
                        version = tag.removePrefix("v"),
                        tag = tag,
                        title = o.optString("name").ifBlank { tag },
                        notes = o.optString("body").ifBlank { "No notes provided." },
                        publishedAt = o.optString("published_at").take(10),
                        apkUrl = apk
                    )
                )
            }
        }
    }

    /** The newest release strictly newer than the installed version, or null if up to date. */
    suspend fun findUpdate(): ReleaseInfo? {
        val current = currentVersion()
        return fetchReleases().firstOrNull { isNewer(it.version, current) }
    }

    /**
     * Downloads [release]'s APK via DownloadManager and launches the system installer when it
     * finishes. Returns false if the release has no APK asset.
     */
    fun downloadAndInstall(release: ReleaseInfo): Boolean {
        val url = release.apkUrl ?: return false
        val fileName = "Kaspotify-${release.tag}.apk"
        // Clear any stale copy so we always install the freshly downloaded file.
        File(context.getExternalFilesDir("updates"), fileName).delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Kaspotify ${release.tag}")
            .setDescription("Downloading update…")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, "updates", fileName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                runCatching { context.unregisterReceiver(this) }
                val file = File(context.getExternalFilesDir("updates"), fileName)
                if (file.exists()) launchInstaller(file)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        return true
    }

    private fun launchInstaller(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kaspotify-App")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val REPO = "KasraJamei/Kaspotify"
        private const val APK_MIME = "application/vnd.android.package-archive"

        /** Semantic-ish compare of dotted version strings ("1.8" newer than "1.7.3"). */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
            val b = current.split(".").map { it.toIntOrNull() ?: 0 }
            val n = maxOf(a.size, b.size)
            for (i in 0 until n) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
