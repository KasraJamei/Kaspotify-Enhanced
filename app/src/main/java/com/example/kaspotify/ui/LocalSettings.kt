package com.example.kaspotify.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.kaspotify.data.settings.AppSettings

/**
 * Provides the current [AppSettings] to the whole UI tree so cross-cutting feature toggles
 * (swipe gestures, toasts, effects/visualizer chips, …) can be read anywhere without prop-drilling.
 * Provided once near the root in MainActivity.
 */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
