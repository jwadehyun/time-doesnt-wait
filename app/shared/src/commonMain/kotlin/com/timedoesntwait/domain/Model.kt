package com.timedoesntwait.domain

import kotlinx.datetime.LocalDate

/** One logged hour: [hour] is the start of the hour (14 = 2–3 PM) on [date]. */
data class HourSlot(val date: LocalDate, val hour: Int) : Comparable<HourSlot> {
    init {
        require(hour in 0..23) { "hour must be 0..23, was $hour" }
    }

    override fun compareTo(other: HourSlot): Int =
        compareValuesBy(this, other, { it.date }, { it.hour })
}

enum class EntrySource { NOTIFICATION_REPLY, QUICK_BUTTON, APP, BACKFILL }

data class Activity(
    val id: String,
    /** User-typed name; for built-in activities this is the English fallback. */
    val name: String,
    /** Built-in activities carry a key so their name can be translated. Null for custom ones. */
    val nameKey: String?,
    val icon: String,
    val keywords: List<String>,
    val sortOrder: Int,
    val isDefault: Boolean,
    val archived: Boolean,
)

data class Vibe(
    val id: String,
    val label: String,
    val labelKey: String?,
    val kaomoji: String,
    val sortOrder: Int,
    val archived: Boolean,
)

data class Entry(
    val id: String,
    val slot: HourSlot,
    val activityId: String?,
    val vibeId: String?,
    val memo: String,
    val source: EntrySource,
    val categoryGuessed: Boolean,
    val loggedAtMillis: Long,
)

data class Goal(
    val id: String,
    val name: String,
    val targetHours: Int,
    val achieved: Boolean,
    /** Number of hours logged toward this goal. May exceed [targetHours]. */
    val progressHours: Int,
)

/** What the Timeline shows for one expected or logged hour. */
sealed interface TimelineRow {
    val slot: HourSlot

    data class Logged(val entry: Entry, val activity: Activity?, val vibe: Vibe?) : TimelineRow {
        override val slot: HourSlot get() = entry.slot

        /** The entry points at an activity that can't be found (shown as UNKNOWN with "?"). */
        val activityMissing: Boolean get() = entry.activityId != null && activity == null
    }

    data class Missed(override val slot: HourSlot) : TimelineRow
}

const val MEMO_MAX_LENGTH = 100
