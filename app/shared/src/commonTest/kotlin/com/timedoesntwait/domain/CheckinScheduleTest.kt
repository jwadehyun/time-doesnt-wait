package com.timedoesntwait.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckinScheduleTest {
    // 2026-11-16 is a Monday.
    private val monday = LocalDate(2026, 11, 16)

    @Test
    fun defaultCoversNineToEightEveryDay() {
        val s = CheckinSchedule.DEFAULT
        DayOfWeek.entries.forEach { day ->
            assertEquals(11, s.pingCount(day), "$day")
            assertTrue(s.isPing(day, 10))
            assertTrue(s.isPing(day, 20))
            assertTrue(!s.isPing(day, 9) && !s.isPing(day, 21))
        }
        assertEquals((9..19).map { HourSlot(monday, it) }, s.expectedSlots(monday))
    }

    @Test
    fun nextPingIsTheNextFullHourThatIsOn() {
        val s = CheckinSchedule.DEFAULT
        assertEquals(LocalDateTime(2026, 11, 16, 10, 0), s.nextPing(LocalDateTime(2026, 11, 16, 9, 59)))
        // Exactly on a ping → the following one, never the same instant twice.
        assertEquals(LocalDateTime(2026, 11, 16, 11, 0), s.nextPing(LocalDateTime(2026, 11, 16, 10, 0)))
        // After the last ping of the day → tomorrow's first.
        assertEquals(LocalDateTime(2026, 11, 17, 10, 0), s.nextPing(LocalDateTime(2026, 11, 16, 20, 0, 5)))
    }

    @Test
    fun perDayHoursAreRespected() {
        val s = CheckinSchedule.everyDay(0)
            .withPing(DayOfWeek.WEDNESDAY, 14, true)
        assertEquals(LocalDateTime(2026, 11, 18, 14, 0), s.nextPing(LocalDateTime(2026, 11, 16, 12, 0)))
        assertEquals(LocalDateTime(2026, 11, 25, 14, 0), s.nextPing(LocalDateTime(2026, 11, 18, 14, 0)))
    }

    @Test
    fun emptyScheduleHasNoPing() {
        assertNull(CheckinSchedule.everyDay(0).nextPing(LocalDateTime(2026, 11, 16, 12, 0)))
    }

    @Test
    fun midnightPingAsksAboutPreviousDaysLastHour() {
        val ping = LocalDateTime(2026, 11, 17, 0, 0)
        assertEquals(HourSlot(monday, 23), slotForPing(ping))
        assertEquals(ping, pingForSlot(HourSlot(monday, 23)))
        val s = CheckinSchedule.everyDay(0).withPing(DayOfWeek.TUESDAY, 0, true)
        assertEquals(listOf(HourSlot(monday, 23)), s.expectedSlots(monday))
    }

    @Test
    fun copyToWeekdays() {
        val s = CheckinSchedule.everyDay(0).withPing(DayOfWeek.MONDAY, 8, true)
            .copyDay(DayOfWeek.MONDAY, CheckinSchedule.WEEKDAYS)
        CheckinSchedule.WEEKDAYS.forEach { assertTrue(s.isPing(it, 8)) }
        CheckinSchedule.WEEKEND.forEach { assertTrue(!s.isPing(it, 8)) }
    }
}
