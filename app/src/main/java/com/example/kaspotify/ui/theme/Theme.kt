package com.example.kaspotify.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Album-art–derived ambient color used only for gradient backdrops / glows.
 * The accent (primary) stays a static platinum so the whole theme never
 * recomposes per-frame — only the few composables that read this local do,
 * and only when the song changes.
 */
// Dynamic (not static) so the per-song ambient animation only recomposes the few composables that
// actually read it (backdrops/mini-player), never the whole app.
val LocalAmbientColor = compositionLocalOf { AmbientNeutral }

private val KaspotifyColorScheme = darkColorScheme(
    primary = Platinum,
    onPrimary = Color.Black,
    secondary = PlatinumDim,
    onSecondary = Color.Black,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = GlassStroke
)

@Composable
fun KaspotifyTheme(
    // Dark-first: this app is always dark.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    /** Optional ambient color (e.g. extracted from current album art) for gradient backdrops. */
    ambientColor: Color? = null,
    content: @Composable () -> Unit
) {
    // Animate the ambient once here so only gradient consumers react, and only on song change.
    val animatedAmbient by animateColorAsState(
        targetValue = ambientColor ?: AmbientNeutral,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "ambient"
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalAmbientColor provides animatedAmbient) {
        MaterialTheme(
            colorScheme = KaspotifyColorScheme,
            typography = Typography,
            content = content
        )
    }
}
