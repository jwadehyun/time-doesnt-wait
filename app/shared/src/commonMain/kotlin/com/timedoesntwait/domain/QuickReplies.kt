package com.timedoesntwait.domain

/** A past reply the user can send again with one tap, plus the activity it was last logged as. */
data class QuickReply(val text: String, val activityId: String?)

/**
 * Turns recent entries (newest first) into reply suggestions, like smart replies in a messaging app:
 * the most-used texts first, ties broken by recency, case-insensitive duplicates merged.
 */
fun buildQuickReplies(recentNewestFirst: List<Pair<String, String?>>, limit: Int = 6): List<QuickReply> {
    data class Acc(val text: String, val activityId: String?, val firstSeen: Int, var count: Int)
    val byKey = LinkedHashMap<String, Acc>()
    recentNewestFirst.forEachIndexed { i, (memo, activityId) ->
        val text = memo.trim()
        if (text.isEmpty() || text.length > 40) return@forEachIndexed // long notes aren't reusable replies
        val key = text.lowercase()
        val acc = byKey.getOrPut(key) { Acc(text, activityId, i, 0) }
        acc.count++
    }
    return byKey.values
        .sortedWith(compareByDescending<Acc> { it.count }.thenBy { it.firstSeen })
        .take(limit)
        .map { QuickReply(it.text, it.activityId) }
}
