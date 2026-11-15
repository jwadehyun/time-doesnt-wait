package com.timedoesntwait.domain

import kotlinx.datetime.LocalDateTime

/**
 * Builds the Timeline for one day: logged entries plus "Not logged" rows for scheduled hours
 * that have already ended and have no entry. Missed hours are computed, never stored.
 * Newest first.
 */
fun buildTimeline(
    schedule: CheckinSchedule,
    entries: List<Entry>,
    activities: Map<String, Activity>,
    vibes: Map<String, Vibe>,
    date: kotlinx.datetime.LocalDate,
    now: LocalDateTime,
): List<TimelineRow> {
    val logged = entries.associateBy { it.slot }
    val missed = schedule.expectedSlots(date)
        .filter { it !in logged && pingForSlot(it) <= now }
        .map { TimelineRow.Missed(it) }
    val rows = entries.map { TimelineRow.Logged(it, it.activityId?.let(activities::get), it.vibeId?.let(vibes::get)) }
    return (rows + missed).sortedByDescending { it.slot }
}

/** The hour a user is most likely logging right now: the one that just ended. */
fun currentSlot(now: LocalDateTime): HourSlot = slotForPing(LocalDateTime(now.date, kotlinx.datetime.LocalTime(now.hour, 0)))
