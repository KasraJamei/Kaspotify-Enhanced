package com.example.kaspotify.ui

import kotlin.random.Random

/**
 * A pool of 30 short greetings shown under the Kaspotify wordmark on Home. Templates containing
 * "%1$s" are personalized with the user's name when one is set; otherwise a neutral fallback is used.
 */
object Greetings {

    private val templates = listOf(
        "Welcome back, %1\$s",
        "Hey %1\$s, ready to listen?",
        "Good to see you, %1\$s",
        "Let's find your sound, %1\$s",
        "What's playing today, %1\$s?",
        "Your music awaits, %1\$s",
        "Press play, %1\$s",
        "Turn it up, %1\$s",
        "Lost in sound, %1\$s?",
        "Make it loud, %1\$s",
        "Hello again, %1\$s",
        "The stage is yours, %1\$s",
        "Feel the rhythm, %1\$s",
        "Back for more, %1\$s?",
        "Let the music move you, %1\$s",
        "Tune in, %1\$s",
        "Good vibes only, %1\$s",
        "Your soundtrack, %1\$s",
        "Time to vibe, %1\$s",
        "Ready when you are, %1\$s",
        "Crank the volume",
        "Every song, offline",
        "Your library, your rules",
        "All your music, one place",
        "Drop the needle",
        "Let it play",
        "Sound on",
        "Find your next favorite",
        "From your collection, with love",
        "Nothing but your music"
    )

    val count: Int get() = templates.size

    /** A random greeting, personalized with [name] if it's not blank. */
    fun random(name: String, seed: Long = System.nanoTime()): String =
        format(templates[Random(seed).nextInt(templates.size)], name)

    private fun format(template: String, name: String): String {
        if (!template.contains("%1\$s")) return template
        val who = name.trim().ifBlank { "friend" }
        return template.format(who)
    }
}
