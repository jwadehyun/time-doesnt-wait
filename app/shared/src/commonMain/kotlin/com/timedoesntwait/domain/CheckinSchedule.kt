package com.timedoesntwait.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Which hours ping on which weekday.
 *
 * A grid cell is a **ping time**: bit H set for a weekday means a check-in at H:00 that asks
 * about the hour before it (cell 10 → ping at 10:00 about 9–10). Cell 0 asks about 23–24
 * of the previous day.
 */
data class CheckinSchedule(private val masks: Map<DayOfWeek, Int>) {

    fun mask(day: DayOfWeek): Int = masks[day] ?: 0

    fun isPing(day: DayOfWeek, hour: Int): Boolean = mask(day) and (1 shl hour) != 0

    fun pingCount(day: DayOfWeek): Int = mask(day).countOneBits()

    fun withPing(day: DayOfWeek, hour: Int, on: Boolean): CheckinSchedule {
        val old = mask(day)
        val new = if (on) old or (1 shl hour) else old and (1 shl hour).inv()
        return copy(masks = masks + (day to new))
    }

    fun withMask(day: DayOfWeek, mask: Int): CheckinSchedule =
        copy(masks = masks + (day to (mask and ALL_HOURS)))

    fun copyDay(from: DayOfWeek, to: Collection<DayOfWeek>): CheckinSchedule =
        copy(masks = masks + to.associateWith { mask(from) })

    /** The first ping strictly after [after], or null if the schedule is empty. */
    fun nextPing(after: LocalDateTime): LocalDateTime? {
        if (masks.values.none { it != 0 }) return null
        var date = after.date
        var hour = after.hour + 1 // pings are on the hour, so the earliest candidate is the next full hour
        repeat(8 * 24) {
            if (hour > 23) {
                hour = 0
                date = date.plus(DatePeriod(days = 1))
            }
            if (isPing(date.dayOfWeek, hour)) return LocalDateTime(date, LocalTime(hour, 0))
            hour++
        }
        return null
    }

    /** Hours of [date] that are expected to be logged, in order. */
    fun expectedSlots(date: LocalDate): List<HourSlot> = buildList {
        for (h in 0..22) if (isPing(date.dayOfWeek, h + 1)) add(HourSlot(date, h))
        val next = date.plus(DatePeriod(days = 1))
        if (isPing(next.dayOfWeek, 0)) add(HourSlot(date, 23))
    }

    companion object {
        const val ALL_HOURS = (1 shl 24) - 1

        /** Pings that cover [dayStart]..[dayEnd]: e.g. 9..20 → pings at 10:00..20:00. */
        fun window(dayStart: Int, dayEnd: Int): Int {
            var mask = 0
            for (h in (dayStart + 1)..dayEnd) mask = mask or (1 shl (h % 24))
            return mask
        }

        fun everyDay(mask: Int): CheckinSchedule =
            CheckinSchedule(DayOfWeek.entries.associateWith { mask and ALL_HOURS })

        /** Default from the design doc: day starts 09:00, ends 20:00, every day. */
        val DEFAULT: CheckinSchedule = everyDay(window(dayStart = 9, dayEnd = 20))

        val WEEKDAYS = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val WEEKEND = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

/** The hour a ping at [ping] asks about. */
fun slotForPing(ping: LocalDateTime): HourSlot =
    if (ping.hour == 0) HourSlot(ping.date.minus(DatePeriod(days = 1)), 23)
    else HourSlot(ping.date, ping.hour - 1)

/** The ping that asks about [slot]. */
fun pingForSlot(slot: HourSlot): LocalDateTime =
    if (slot.hour == 23) LocalDateTime(slot.date.plus(DatePeriod(days = 1)), LocalTime(0, 0))
    else LocalDateTime(slot.date, LocalTime(slot.hour + 1, 0))

fun DayOfWeek.isoNumber(): Int = isoDayNumber
