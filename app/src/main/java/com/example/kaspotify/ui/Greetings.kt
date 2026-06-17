package com.example.kaspotify.ui

import kotlin.random.Random

/**
 * A pool of 30 short greetings shown under the Kaspotify wordmark on Home. Templates containing
 * "%1$s" are personalized with the user's name when one is set; otherwise a neutral fallback is used.
 */
object Greetings {

    private val templates = listOf(
        "Welcome back, %1\$s",
        "The evening is yours, %1\$s",
        "Let the music find you, %1\$s",
        "A quiet moment, a perfect song, %1\$s",
        "Where shall we wander tonight, %1\$s?",
        "Your symphony awaits, %1\$s",
        "Settle in, %1\$s",
        "Every good day deserves a soundtrack, %1\$s",
        "Compose your mood, %1\$s",
        "The needle's ready when you are, %1\$s",
        "Good to have you back, %1\$s",
        "Let's set the tone, %1\$s",
        "Something beautiful is about to play, %1\$s",
        "Drift away, %1\$s",
        "Your encore begins, %1\$s",
        "Press play on the moment",
        "A world of sound, all your own",
        "Quiet the noise, find the music",
        "Let melody do the talking",
        "Every song, exactly where you left it",
        "Lose yourself in the sound",
        "The night is young, the playlist younger",
        "Turn the everyday into a soundtrack",
        "Beautiful sound, no strings attached",
        "Your library, gently waiting",
        "Let the first note set you free",
        "Made for slow mornings and long drives",
        "Sound, savored",
        "An ode to the songs you love",
        "Hush — then music"
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
