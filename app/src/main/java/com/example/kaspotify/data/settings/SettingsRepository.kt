package com.example.kaspotify.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** User-toggleable feature flags. Everything defaults on so the app behaves richly out of the box. */
data class AppSettings(
    val albumArtTheming: Boolean = true,
    val highRefreshRate: Boolean = true,
    val inAppToasts: Boolean = true,
    val listSwipeGestures: Boolean = true,
    val audioEffects: Boolean = true,
    val visualizer: Boolean = true,
    /** Whether per-song quality badges (Lossless, 320 kbps, …) are shown in lists and Now Playing. */
    val showQualityBadges: Boolean = true,
    /** Even out perceived loudness across tracks (LoudnessEnhancer make-up gain). */
    val volumeNormalization: Boolean = false,
    /** Hearing safety: warn once when output is dangerously loud. */
    val highVolumeWarning: Boolean = true,
    /** Hearing safety: suggest a break after a long continuous loud session. */
    val listeningTimeReminder: Boolean = true,
    /** Hearing safety: cap playback output to [maxVolumePercent]. */
    val maxVolumeCap: Boolean = false,
    /** Output ceiling (percent) used when [maxVolumeCap] is on. */
    val maxVolumePercent: Int = 85,
    /** Display name the user set for personalized greetings (blank = not set). */
    val userName: String = "",
    /** App language: "system", "en", or "fa". */
    val language: String = "system",
    /** Whether the first-launch welcome guide has been shown/dismissed. */
    val onboardingSeen: Boolean = false,
    /** Whether the interactive coach-mark tour has been shown/dismissed. */
    val tourSeen: Boolean = false
)

/**
 * Persists [AppSettings] in SharedPreferences and exposes them as a [StateFlow]. SharedPreferences
 * (rather than DataStore) keeps the dependency surface tiny; writes are synchronous-cheap booleans.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kaspotify_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        albumArtTheming = prefs.getBoolean(KEY_THEMING, true),
        highRefreshRate = prefs.getBoolean(KEY_HIGH_REFRESH, true),
        inAppToasts = prefs.getBoolean(KEY_TOASTS, true),
        listSwipeGestures = prefs.getBoolean(KEY_SWIPE, true),
        audioEffects = prefs.getBoolean(KEY_EFFECTS, true),
        visualizer = prefs.getBoolean(KEY_VISUALIZER, true),
        showQualityBadges = prefs.getBoolean(KEY_QUALITY_BADGES, true),
        volumeNormalization = prefs.getBoolean(KEY_NORMALIZE, false),
        highVolumeWarning = prefs.getBoolean(KEY_VOL_WARN, true),
        listeningTimeReminder = prefs.getBoolean(KEY_BREAK_REMINDER, true),
        maxVolumeCap = prefs.getBoolean(KEY_VOL_CAP, false),
        maxVolumePercent = prefs.getInt(KEY_VOL_CAP_PCT, 85),
        userName = prefs.getString(KEY_USER_NAME, "") ?: "",
        language = prefs.getString(KEY_LANGUAGE, "system") ?: "system",
        onboardingSeen = prefs.getBoolean(KEY_ONBOARDING, false),
        tourSeen = prefs.getBoolean(KEY_TOUR, false)
    )

    private fun put(key: String, value: Boolean, apply: (AppSettings) -> AppSettings) {
        prefs.edit().putBoolean(key, value).apply()
        _settings.value = apply(_settings.value)
    }

    fun setMaxVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        prefs.edit().putInt(KEY_VOL_CAP_PCT, clamped).apply()
        _settings.value = _settings.value.copy(maxVolumePercent = clamped)
    }

    fun setAlbumArtTheming(v: Boolean) = put(KEY_THEMING, v) { it.copy(albumArtTheming = v) }
    fun setHighRefreshRate(v: Boolean) = put(KEY_HIGH_REFRESH, v) { it.copy(highRefreshRate = v) }
    fun setInAppToasts(v: Boolean) = put(KEY_TOASTS, v) { it.copy(inAppToasts = v) }
    fun setListSwipeGestures(v: Boolean) = put(KEY_SWIPE, v) { it.copy(listSwipeGestures = v) }
    fun setAudioEffects(v: Boolean) = put(KEY_EFFECTS, v) { it.copy(audioEffects = v) }
    fun setVisualizer(v: Boolean) = put(KEY_VISUALIZER, v) { it.copy(visualizer = v) }
    fun setShowQualityBadges(v: Boolean) = put(KEY_QUALITY_BADGES, v) { it.copy(showQualityBadges = v) }
    fun setVolumeNormalization(v: Boolean) = put(KEY_NORMALIZE, v) { it.copy(volumeNormalization = v) }
    fun setHighVolumeWarning(v: Boolean) = put(KEY_VOL_WARN, v) { it.copy(highVolumeWarning = v) }
    fun setListeningTimeReminder(v: Boolean) = put(KEY_BREAK_REMINDER, v) { it.copy(listeningTimeReminder = v) }
    fun setMaxVolumeCap(v: Boolean) = put(KEY_VOL_CAP, v) { it.copy(maxVolumeCap = v) }
    fun setUserName(name: String) {
        val trimmed = name.trim().take(24)
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        _settings.value = _settings.value.copy(userName = trimmed)
    }
    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _settings.value = _settings.value.copy(language = lang)
    }
    fun setOnboardingSeen(v: Boolean) = put(KEY_ONBOARDING, v) { it.copy(onboardingSeen = v) }
    fun setTourSeen(v: Boolean) = put(KEY_TOUR, v) { it.copy(tourSeen = v) }

    private companion object {
        const val KEY_THEMING = "album_art_theming"
        const val KEY_HIGH_REFRESH = "high_refresh_rate"
        const val KEY_TOASTS = "in_app_toasts"
        const val KEY_SWIPE = "list_swipe_gestures"
        const val KEY_EFFECTS = "audio_effects"
        const val KEY_VISUALIZER = "visualizer"
        const val KEY_QUALITY_BADGES = "show_quality_badges"
        const val KEY_NORMALIZE = "volume_normalization"
        const val KEY_VOL_WARN = "high_volume_warning"
        const val KEY_BREAK_REMINDER = "listening_time_reminder"
        const val KEY_VOL_CAP = "max_volume_cap"
        const val KEY_VOL_CAP_PCT = "max_volume_percent"
        const val KEY_USER_NAME = "user_name"
        const val KEY_LANGUAGE = "language"
        const val KEY_ONBOARDING = "onboarding_seen"
        const val KEY_TOUR = "tour_seen"
    }
}
