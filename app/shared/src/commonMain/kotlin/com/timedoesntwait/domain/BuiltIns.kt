package com.timedoesntwait.domain

/** The built-in activities, vibes and ratings seeded on first launch. */
object BuiltIns {
    /** (name key, English name, icon) */
    val ACTIVITIES = listOf(
        Triple("grind", "Grind", "bolt"),
        Triple("snack", "Snack", "bowl"),
        Triple("vibe", "Vibe", "headphones"),
        Triple("study", "Study", "book"),
        Triple("walk", "Walk", "walk"),
        Triple("nap", "Nap", "moon"),
        Triple("game", "Game", "gamepad"),
        Triple("workout", "Work out", "dumbbell"),
    )

    /**
     * Built-ins no longer seeded. Existing installs keep the row (archived if untouched) so past
     * hours still show it, translated.
     */
    val RETIRED_ACTIVITIES = listOf(
        Triple("chat", "Chat", "chat"), // replaced by "workout" (2026-11-17)
    )

    /** (label key, English label, kaomoji) */
    val VIBES = listOf(
        Triple("super", "Super", "(★‿★)"),
        Triple("solid", "Solid", "(•̀ᴗ•́)"),
        Triple("chill", "Chill", "(￣ー￣)"),
        Triple("tired", "Tired", "(－_－)zZ"),
    )

    /** (name key, English name, kind) */
    val RATINGS = listOf(
        Triple("productivity", "Productivity", "STARS_5"),
        Triple("happiness", "Happiness", "HEARTS_5"),
        Triple("progress", "Progress", "SEGMENTS_10"),
    )

    /** Icon shown for an entry whose activity no longer exists at all. */
    const val UNKNOWN_ICON = "question"
}

/**
 * A built-in activity keeps its translated name (Grind / 업무) until the user renames it.
 * After a rename the user's text is shown in every language, but the built-in keyword list
 * (nameKey) still helps category guessing.
 */
val Activity.usesBuiltInName: Boolean
    get() = nameKey != null &&
        (BuiltIns.ACTIVITIES + BuiltIns.RETIRED_ACTIVITIES).any { it.first == nameKey && it.second == name }

/** Same rule for built-in vibes (Super / 최고). */
val Vibe.usesBuiltInLabel: Boolean
    get() = labelKey != null && BuiltIns.VIBES.any { it.first == labelKey && it.second == label }
