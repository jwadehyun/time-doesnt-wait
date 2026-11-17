package com.timedoesntwait.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsTest {
    private val mon = LocalDate(2026, 11, 16) // a Monday

    @Test
    fun weeksStartOnMondayAndMonthsAreCalendarMonths() {
        val week = DateRange.containing(LocalDate(2026, 11, 19), StatsPeriod.WEEK)
        assertEquals(DateRange(mon, LocalDate(2026, 11, 22)), week)
        assertEquals(DateRange(mon, LocalDate(2026, 11, 22)), DateRange.containing(LocalDate(2026, 11, 22), StatsPeriod.WEEK))
        assertEquals(DateRange(LocalDate(2026, 11, 9), LocalDate(2026, 11, 15)), week.shift(StatsPeriod.WEEK, -1))

        val feb = DateRange.containing(LocalDate(2028, 2, 10), StatsPeriod.MONTH)
        assertEquals(DateRange(LocalDate(2028, 2, 1), LocalDate(2028, 2, 29)), feb)
        assertEquals(29, feb.dayCount)
        assertEquals(LocalDate(2028, 1, 1), feb.shift(StatsPeriod.MONTH, -1).start)
    }

    @Test
    fun streakCountsDaysWithAtLeastOneLoggedHour() {
        val dates = listOf(
            LocalDate(2026, 11, 1), LocalDate(2026, 11, 2), LocalDate(2026, 11, 3), LocalDate(2026, 11, 4), // best: 4
            LocalDate(2026, 11, 13), LocalDate(2026, 11, 14), LocalDate(2026, 11, 15),
        )
        // Today (16th) not logged yet: the streak still runs up to yesterday.
        assertEquals(Streak(current = 3, best = 4), StatsCalculator.streak(dates, mon))
        // Logging today extends it.
        assertEquals(Streak(current = 4, best = 4), StatsCalculator.streak(dates + mon, mon))
        // A missed yesterday breaks it.
        assertEquals(Streak(current = 0, best = 4), StatsCalculator.streak(dates, LocalDate(2026, 11, 17)))
        assertEquals(Streak(0, 0), StatsCalculator.streak(emptyList(), mon))
    }

    @Test
    fun computesTotalsRatingsAndActivitySplit() {
        val wed = LocalDate(2026, 11, 18)
        val range = DateRange.containing(wed, StatsPeriod.WEEK)
        val previous = range.shift(StatsPeriod.WEEK, -1)
        val hours = buildList {
            repeat(11) { add(LoggedHour(mon, 9 + it, "work")) } // Monday fully logged (11 of 11)
            add(LoggedHour(LocalDate(2026, 11, 17), 9, "work"))
            add(LoggedHour(LocalDate(2026, 11, 17), 10, null))
            add(LoggedHour(wed, 9, "gym"))
        }
        val previousHours = buildList {
            repeat(5) { add(LoggedHour(LocalDate(2026, 11, 9), 9 + it, "work")) } // last Mon–Wed: 5 + 2
            repeat(2) { add(LoggedHour(LocalDate(2026, 11, 11), 9 + it, "work")) }
            repeat(4) { add(LoggedHour(LocalDate(2026, 11, 12), 9 + it, "work")) } // last Thursday: not compared yet
        }
        val stats = StatsCalculator.compute(
            range = range,
            previousRange = previous,
            today = wed,
            hours = hours,
            previousHours = previousHours,
            ratings = mapOf(mon to 4, LocalDate(2026, 11, 17) to 3, LocalDate(2026, 11, 10) to 1),
            schedule = CheckinSchedule.DEFAULT,
            loggedDates = listOf(mon, LocalDate(2026, 11, 17), wed),
        )

        assertEquals(14, stats.totalHours)
        assertEquals(7, stats.previousHours, "mid-week compares Mon–Wed with last Mon–Wed")
        assertEquals(7, stats.hoursChange)
        assertEquals(3.5, stats.averageRating)
        assertEquals(2, stats.ratedDays)
        assertEquals(Streak(3, 3), stats.streak)

        assertEquals(7, stats.days.size)
        assertTrue(stats.days[0].fullyLogged)
        assertEquals(4, stats.days[0].rating)
        assertEquals(11, stats.days[1].expectedHours)
        assertTrue(stats.days[2].isToday)
        assertTrue(stats.days[3].isFuture)
        assertNull(stats.days[3].rating)

        assertEquals(listOf("work", "gym", null), stats.activities.map { it.activityId })
        assertEquals(12, stats.activities[0].hours)
        assertEquals(1f / 14, stats.activities[2].fraction)
    }

    @Test
    fun finishedPeriodsCompareWholePeriods() {
        val range = DateRange.containing(LocalDate(2026, 10, 15), StatsPeriod.MONTH)
        val previous = range.shift(StatsPeriod.MONTH, -1)
        val stats = StatsCalculator.compute(
            range, previous, today = LocalDate(2026, 11, 16),
            hours = listOf(LoggedHour(LocalDate(2026, 10, 31), 9, null)),
            previousHours = listOf(LoggedHour(LocalDate(2026, 9, 30), 9, null), LoggedHour(LocalDate(2026, 9, 1), 9, null)),
            ratings = emptyMap(),
            schedule = CheckinSchedule.DEFAULT,
            loggedDates = emptyList(),
        )
        assertEquals(31, stats.days.size)
        assertEquals(2, stats.previousHours)
        assertEquals(-1, stats.hoursChange)
        assertNull(stats.averageRating)
    }

    @Test
    fun lateNightOpensAnUnreviewedYesterday() {
        val night = LocalDateTime(2026, 11, 17, 0, 40)
        assertEquals(mon, defaultReviewDate(night, yesterdayReviewed = false))
        assertEquals(LocalDate(2026, 11, 17), defaultReviewDate(night, yesterdayReviewed = true))
        assertEquals(LocalDate(2026, 11, 17), defaultReviewDate(LocalDateTime(2026, 11, 17, 4, 0), yesterdayReviewed = false))
    }

    @Test
    fun rangeLabels() {
        assertEquals("Nov 16–22", TimeLabels.dayRange(mon, LocalDate(2026, 11, 22), AppLanguage.EN))
        assertEquals("Oct 27 – Nov 2", TimeLabels.dayRange(LocalDate(2026, 10, 27), LocalDate(2026, 11, 2), AppLanguage.EN))
        assertEquals("11월 16일–22일", TimeLabels.dayRange(mon, LocalDate(2026, 11, 22), AppLanguage.KO))
        assertEquals("2026년 11월", TimeLabels.month(mon, AppLanguage.KO))
        assertEquals("4.1", TimeLabels.oneDecimal(4.125))
        assertEquals("3.0", TimeLabels.oneDecimal(3.0))
    }
}
