package com.timedoesntwait.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class StatsPeriod { WEEK, MONTH }

/** Inclusive range of days. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    val dayCount: Int get() = start.daysUntil(end) + 1
    val days: List<LocalDate> get() = List(dayCount) { start.plus(DatePeriod(days = it)) }
    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end

    companion object {
        /** The week (Monday–Sunday) or calendar month that holds [date]. */
        fun containing(date: LocalDate, period: StatsPeriod): DateRange = when (period) {
            StatsPeriod.WEEK -> {
                val monday = date.minus(DatePeriod(days = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal))
                DateRange(monday, monday.plus(DatePeriod(days = 6)))
            }
            StatsPeriod.MONTH -> {
                val first = LocalDate(date.year, date.month, 1)
                DateRange(first, first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)))
            }
        }
    }
}

fun DateRange.shift(period: StatsPeriod, by: Int): DateRange = when (period) {
    StatsPeriod.WEEK -> DateRange.containing(start.plus(DatePeriod(days = 7 * by)), period)
    StatsPeriod.MONTH -> DateRange.containing(start.plus(DatePeriod(months = by)), period)
}

/** One logged hour, as Stats needs it. */
data class LoggedHour(val date: LocalDate, val hour: Int, val activityId: String?)

data class DayStat(
    val date: LocalDate,
    val loggedHours: Int,
    /** Hours the check-in schedule asks about on this day. */
    val expectedHours: Int,
    /** Overall rating from that day's review, if saved. */
    val rating: Int?,
    val isToday: Boolean,
    val isFuture: Boolean,
) {
    val fullyLogged: Boolean get() = expectedHours > 0 && loggedHours >= expectedHours
}

/** Hours logged under one activity. [activityId] null = Uncategorized. */
data class ActivityShare(val activityId: String?, val hours: Int, val fraction: Float)

data class Streak(val current: Int, val best: Int)

data class PeriodStats(
    val range: DateRange,
    val days: List<DayStat>,
    val totalHours: Int,
    /** Hours logged by the same point of the previous period (the whole period if this one is over). */
    val previousHours: Int,
    val averageRating: Double?,
    val ratedDays: Int,
    val streak: Streak,
    val activities: List<ActivityShare>,
) {
    val hoursChange: Int get() = totalHours - previousHours
}

object StatsCalculator {

    /**
     * A day counts toward the streak when it has at least one logged hour. Today doesn't break the
     * current streak while it is still empty: the streak then runs up to yesterday.
     */
    fun streak(loggedDates: Collection<LocalDate>, today: LocalDate): Streak {
        val dates = loggedDates.filter { it <= today }.toSortedSet()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in dates) {
            run = if (prev != null && prev.plus(DatePeriod(days = 1)) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        var current = 0
        var day = if (today in dates) today else today.minus(DatePeriod(days = 1))
        while (day in dates) {
            current++
            day = day.minus(DatePeriod(days = 1))
        }
        return Streak(current, best)
    }

    fun compute(
        range: DateRange,
        previousRange: DateRange,
        today: LocalDate,
        hours: List<LoggedHour>,
        previousHours: List<LoggedHour>,
        ratings: Map<LocalDate, Int>,
        schedule: CheckinSchedule,
        loggedDates: Collection<LocalDate>,
    ): PeriodStats {
        val inRange = hours.filter { it.date in range }
        val perDay = inRange.groupingBy { it.date }.eachCount()
        val days = range.days.map { d ->
            DayStat(
                date = d,
                loggedHours = perDay[d] ?: 0,
                expectedHours = schedule.expectedSlots(d).size,
                rating = ratings[d],
                isToday = d == today,
                isFuture = d > today,
            )
        }

        // Compare like with like: mid-week, this week so far vs. the same days of last week.
        val previousCutoff = if (today in range) {
            previousRange.start.plus(DatePeriod(days = range.start.daysUntil(today))).coerceAtMost(previousRange.end)
        } else {
            previousRange.end
        }
        val previousTotal = previousHours.count { it.date >= previousRange.start && it.date <= previousCutoff }

        val periodRatings = ratings.filterKeys { it in range }.values
        val total = inRange.size
        val shares = inRange.groupingBy { it.activityId }.eachCount()
            .map { (id, n) -> ActivityShare(id, n, if (total == 0) 0f else n.toFloat() / total) }
            .sortedWith(compareByDescending<ActivityShare> { it.hours }.thenBy { it.activityId == null })

        return PeriodStats(
            range = range,
            days = days,
            totalHours = total,
            previousHours = previousTotal,
            averageRating = if (periodRatings.isEmpty()) null else periodRatings.average(),
            ratedDays = periodRatings.size,
            streak = streak(loggedDates, today),
            activities = shares,
        )
    }
}
