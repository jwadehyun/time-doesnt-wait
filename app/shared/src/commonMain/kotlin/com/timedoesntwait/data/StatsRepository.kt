package com.timedoesntwait.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.DateRange
import com.timedoesntwait.domain.LoggedHour
import com.timedoesntwait.domain.PeriodStats
import com.timedoesntwait.domain.StatsCalculator
import com.timedoesntwait.domain.StatsPeriod
import com.timedoesntwait.domain.shift
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Read-only numbers for the Stats tab. Updates live as hours, reviews or the schedule change. */
class StatsRepository(
    db: AppDatabase,
    private val schedule: ScheduleRepository,
    private val io: CoroutineDispatcher,
) {
    private val q = db.journalQueries

    fun stats(range: DateRange, period: StatsPeriod, today: LocalDate): Flow<PeriodStats> {
        val previous = range.shift(period, -1)
        val hours = q.entriesBetween(previous.start.toString(), range.end.toString()).asFlow().mapToList(io)
            .map { rows -> rows.map { LoggedHour(LocalDate.parse(it.local_date), it.hour.toInt(), it.activity_id) } }
        val ratings = q.reviewsBetween(range.start.toString(), range.end.toString()).asFlow().mapToList(io)
            .map { rows -> rows.associate { LocalDate.parse(it.local_date) to it.overall_rating.toInt() } }
        val loggedDates = q.loggedDates().asFlow().mapToList(io).map { rows -> rows.map(LocalDate::parse) }
        return combine(hours, ratings, loggedDates, schedule.schedule()) { h, r, dates, s ->
            StatsCalculator.compute(
                range = range,
                previousRange = previous,
                today = today,
                hours = h.filter { it.date in range },
                previousHours = h.filter { it.date in previous },
                ratings = r,
                schedule = s,
                loggedDates = dates,
            )
        }
    }

    /** Hours logged toward each goal (by goal id) within [range]. */
    fun goalHours(range: DateRange): Flow<Map<String, Int>> =
        q.goalHoursBetween(range.start.toString(), range.end.toString()).asFlow().mapToList(io)
            .map { rows -> rows.associate { it.goal_id to it.hours.toInt() } }
}
