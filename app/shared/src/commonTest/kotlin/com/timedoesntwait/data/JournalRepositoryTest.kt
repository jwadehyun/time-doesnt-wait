package com.timedoesntwait.data

import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.CheckinSchedule
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.TimelineRow
import com.timedoesntwait.domain.buildTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class JournalRepositoryTest {
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(1_794_640_804_050)
    }
    private lateinit var db: AppDatabase
    private lateinit var repo: JournalRepository
    private val day = LocalDate(2026, 11, 16)

    @BeforeTest
    fun setUp() {
        db = AppDatabase(testDriverFactory().create())
        repo = JournalRepository(db, clock, Dispatchers.Unconfined)
        repo.seedDefaultsIfNeeded()
    }

    @Test
    fun seedsDefaultsOnce() = runTest {
        repo.seedDefaultsIfNeeded()
        assertEquals(8, repo.activities().first().size)
        assertEquals(4, repo.vibes().first().size)
    }

    @Test
    fun oneEntryPerHourAndEveryWriteIsQueued() = runTest {
        val slot = HourSlot(day, 14)
        val logged = mutableListOf<HourSlot>()
        repo.onSlotLogged = { slot, _ -> logged += slot }

        val first = repo.logText(slot, "ramen for lunch", EntrySource.NOTIFICATION_REPLY)
        val snack = repo.activities().first().first { it.nameKey == "snack" }
        assertEquals(snack.id, first.activityId)
        assertTrue(first.categoryGuessed)

        val second = repo.logText(slot, "actually a nap", EntrySource.APP)
        assertEquals(first.id, second.id, "same hour updates the same row")
        assertEquals("actually a nap", second.memo)
        assertEquals(1, repo.entriesFor(day).first().size)
        assertEquals(listOf(slot, slot), logged)

        repo.deleteEntry(second.id)
        assertNull(repo.entryFor(slot))
        assertTrue(repo.outboxCount().first() > 0)
    }

    @Test
    fun unknownTextIsUncategorized() = runTest {
        val e = repo.logText(HourSlot(day, 10), "stuff", EntrySource.NOTIFICATION_REPLY)
        assertNull(e.activityId)
    }

    @Test
    fun memoIsCappedAt100() = runTest {
        val e = repo.saveEntry(HourSlot(day, 9), null, null, "x".repeat(250), EntrySource.APP)
        assertEquals(100, e.memo.length)
    }

    @Test
    fun goalProgressCanExceedTarget() = runTest {
        val goalId = repo.addGoal("Read", targetHours = 1)
        repo.saveEntry(HourSlot(day, 9), null, null, "", EntrySource.APP, goalIds = setOf(goalId))
        repo.saveEntry(HourSlot(day, 10), null, null, "", EntrySource.APP, goalIds = setOf(goalId))
        val goal = repo.goals().first().single()
        assertEquals(2, goal.progressHours)
        assertEquals(false, goal.achieved, "achieved is the user's call, not automatic")

        // Un-ticking the goal for an hour removes that hour's progress.
        repo.saveEntry(HourSlot(day, 10), null, null, "", EntrySource.APP, goalIds = emptySet())
        assertEquals(1, repo.goals().first().single().progressHours)
        repo.saveEntry(HourSlot(day, 10), null, null, "", EntrySource.APP, goalIds = setOf(goalId))
        assertEquals(2, repo.goals().first().single().progressHours)

        repo.setGoalAchieved(goalId, true)
        assertTrue(repo.goals().first().single().achieved)
    }

    @Test
    fun timelineShowsMissedPastHoursOnly() = runTest {
        repo.logText(HourSlot(day, 12), "lunch", EntrySource.APP)
        val rows = buildTimeline(
            schedule = CheckinSchedule.DEFAULT,
            entries = repo.entriesFor(day).first(),
            activities = repo.activities().first().associateBy { it.id },
            vibes = emptyMap(),
            date = day,
            now = LocalDateTime(2026, 11, 16, 14, 30),
        )
        // Pings so far: 10:00, 11:00, 12:00, 13:00, 14:00 → hours 9..13. 12 is logged.
        assertEquals(listOf(13, 12, 11, 10, 9), rows.map { it.slot.hour })
        assertTrue(rows.single { it.slot.hour == 12 } is TimelineRow.Logged)
        assertEquals(4, rows.count { it is TimelineRow.Missed })
    }

    @Test
    fun topActivitiesFollowUsage() = runTest {
        val acts = repo.activities().first()
        val walk = acts.first { it.nameKey == "walk" }
        val chat = acts.first { it.nameKey == "chat" }
        listOf(9, 10, 11).forEach { repo.logActivity(HourSlot(day, it), walk.id, EntrySource.APP) }
        repo.logActivity(HourSlot(day, 12), chat.id, EntrySource.APP)
        assertEquals(listOf(walk.id, chat.id), repo.topActivities(day).map { it.id })
    }
}
