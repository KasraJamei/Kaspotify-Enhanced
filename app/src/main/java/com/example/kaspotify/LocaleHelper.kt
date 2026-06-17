package com.example.kaspotify

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies a per-app language by wrapping the base context with a chosen locale. Read straight from
 * SharedPreferences (not Hilt) because it runs in [android.app.Activity.attachBaseContext], before
 * dependency injection is available. "system" leaves the device locale untouched.
 */
object LocaleHelper {

    fun wrap(base: Context): Context {
        val lang = base
            .getSharedPreferences("kaspotify_settings", Context.MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        if (lang == "system") return base

        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
