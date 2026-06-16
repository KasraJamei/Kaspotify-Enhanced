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
        onboardingSeen = prefs.getBoolean(KEY_ONBOARDING, false),
        tourSeen = prefs.getBoolean(KEY_TOUR, false)
    )

    private fun put(key: String, value: Boolean, apply: (AppSettings) -> AppSettings) {
        prefs.edit().putBoolean(key, value).apply()
        _settings.value = apply(_settings.value)
    }

    fun setAlbumArtTheming(v: Boolean) = put(KEY_THEMING, v) { it.copy(albumArtTheming = v) }
    fun setHighRefreshRate(v: Boolean) = put(KEY_HIGH_REFRESH, v) { it.copy(highRefreshRate = v) }
    fun setInAppToasts(v: Boolean) = put(KEY_TOASTS, v) { it.copy(inAppToasts = v) }
    fun setListSwipeGestures(v: Boolean) = put(KEY_SWIPE, v) { it.copy(listSwipeGestures = v) }
    fun setAudioEffects(v: Boolean) = put(KEY_EFFECTS, v) { it.copy(audioEffects = v) }
    fun setVisualizer(v: Boolean) = put(KEY_VISUALIZER, v) { it.copy(visualizer = v) }
    fun setOnboardingSeen(v: Boolean) = put(KEY_ONBOARDING, v) { it.copy(onboardingSeen = v) }
    fun setTourSeen(v: Boolean) = put(KEY_TOUR, v) { it.copy(tourSeen = v) }

    private companion object {
        const val KEY_THEMING = "album_art_theming"
        const val KEY_HIGH_REFRESH = "high_refresh_rate"
        const val KEY_TOASTS = "in_app_toasts"
        const val KEY_SWIPE = "list_swipe_gestures"
        const val KEY_EFFECTS = "audio_effects"
        const val KEY_VISUALIZER = "visualizer"
        const val KEY_ONBOARDING = "onboarding_seen"
        const val KEY_TOUR = "tour_seen"
    }
}
