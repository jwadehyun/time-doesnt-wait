package com.timedoesntwait.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.db.Checkin_schedule
import com.timedoesntwait.domain.CheckinSchedule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlin.time.Clock

class ScheduleRepository(
    private val db: AppDatabase,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
) {
    private val q = db.journalQueries

    /** Called after the schedule changes, so the next alarm can be rescheduled. */
    var onScheduleChanged: () -> Unit = {}

    fun schedule(): Flow<CheckinSchedule> =
        q.scheduleRows().asFlow().mapToList(io).map { it.toSchedule() }

    /** Synchronous read for alarm receivers. */
    fun current(): CheckinSchedule = q.scheduleRows().executeAsList().toSchedule()

    fun seedIfEmpty() {
        if (q.scheduleRows().executeAsList().isEmpty()) writeAll(CheckinSchedule.DEFAULT)
    }

    suspend fun save(schedule: CheckinSchedule) {
        withContext(io) { writeAll(schedule) }
        onScheduleChanged()
    }

    private fun writeAll(schedule: CheckinSchedule) {
        db.transaction {
            val t = clock.now().toEpochMilliseconds()
            DayOfWeek.entries.forEach { day ->
                val weekday = (day.ordinal + 1).toLong()
                q.upsertSchedule(weekday, schedule.mask(day).toLong(), t)
                q.enqueue("checkin_schedule", weekday.toString(), t)
            }
        }
    }

    private fun List<Checkin_schedule>.toSchedule(): CheckinSchedule =
        if (isEmpty()) CheckinSchedule.DEFAULT
        else CheckinSchedule(associate { DayOfWeek.entries[(it.weekday - 1).toInt()] to it.hours_mask.toInt() })
}
