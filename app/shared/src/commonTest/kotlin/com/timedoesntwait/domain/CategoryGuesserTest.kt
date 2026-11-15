package com.timedoesntwait.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryGuesserTest {
    private val activities = listOf("grind", "snack", "vibe", "study", "walk", "nap", "game", "chat")
        .mapIndexed { i, key ->
            Activity("id-$key", key.replaceFirstChar { it.uppercase() }, key, key, emptyList(), i, true, false)
        } + Activity("id-guitar", "Guitar", null, "star", listOf("practice guitar", "기타"), 9, false, false)

    private val guesser = CategoryGuesser(activities)
    private fun guess(text: String) = guesser.guess(text)?.nameKey ?: guesser.guess(text)?.name

    @Test
    fun english() {
        assertEquals("snack", guess("Spicy ramen noodles with eggs"))
        assertEquals("grind", guess("Sprint review meeting, presentation went well"))
        assertEquals("walk", guess("Quick 15 min sun walk"))
        assertEquals("nap", guess("20 min power nap"))
        assertEquals("study", guess("Finished chapter 4 of algorithms"))
    }

    @Test
    fun korean() {
        assertEquals("snack", guess("점심으로 라면 먹었다"))
        assertEquals("walk", guess("한강 산책했어"))
        assertEquals("chat", guess("엄마랑 통화"))
        assertEquals("study", guess("영어 공부"))
    }

    @Test
    fun customActivityNameAndKeywords() {
        assertEquals("Guitar", guess("guitar session"))
        assertEquals("Guitar", guess("기타 연습"))
    }

    @Test
    fun noMatchOrTieIsUncategorized() {
        assertNull(guess("hmm"))
        assertNull(guess(""))
        // "lunch" (snack) and "meeting" (grind) tie → uncategorized.
        assertNull(guess("lunch meeting"))
    }
}
