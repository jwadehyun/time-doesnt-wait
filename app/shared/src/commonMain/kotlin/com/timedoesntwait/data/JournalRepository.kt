package com.timedoesntwait.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.BuiltIns
import com.timedoesntwait.domain.QuickReply
import com.timedoesntwait.domain.buildQuickReplies
import com.timedoesntwait.domain.CategoryGuesser
import com.timedoesntwait.domain.Entry
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.Goal
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.Ids
import com.timedoesntwait.domain.MEMO_MAX_LENGTH
import com.timedoesntwait.domain.Vibe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Clock
import com.timedoesntwait.db.Activity as ActivityRow
import com.timedoesntwait.db.Entry as EntryRow
import com.timedoesntwait.db.Vibe as VibeRow

/**
 * The local database is the source of truth. Every write runs in one transaction that also
 * queues the changed row in the outbox for sync.
 */
class JournalRepository(
    private val db: AppDatabase,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
) {
    private val q = db.journalQueries

    /** Called after an hour gets an entry (e.g. to clear that hour's notification). */
    var onSlotLogged: (HourSlot, EntrySource) -> Unit = { _, _ -> }

    private fun now() = clock.now().toEpochMilliseconds()

    // ── seed ────────────────────────────────────────────────

    fun seedDefaultsIfNeeded() {
        db.transaction {
            val t = now()
            if (q.activityCount().executeAsOne() == 0L) {
                BuiltIns.ACTIVITIES.forEachIndexed { i, (key, name, icon) ->
                    val id = Ids.uuidV7(t)
                    q.insertActivity(id, name, key, icon, "", (i + 1).toLong(), 1, t)
                    q.enqueue("activity", id, t)
                }
                BuiltIns.VIBES.forEachIndexed { i, (key, label, kaomoji) ->
                    val id = Ids.uuidV7(t)
                    q.insertVibe(id, label, key, kaomoji, (i + 1).toLong(), t)
                    q.enqueue("vibe", id, t)
                }
            }
            if (q.ratingDimensionCount().executeAsOne() == 0L) {
                BuiltIns.RATINGS.forEachIndexed { i, (key, name, kind) ->
                    val id = Ids.uuidV7(t)
                    q.insertRatingDimension(id, name, key, kind, (i + 1).toLong(), 1, t)
                    q.enqueue("rating_dimension", id, t)
                }
            }
        }
    }

    // ── reads ───────────────────────────────────────────────

    fun activities(): Flow<List<Activity>> =
        q.activeActivities().asFlow().mapToList(io).mapRows { it.toModel() }

    fun allActivities(): Flow<List<Activity>> =
        q.allActivities().asFlow().mapToList(io).mapRows { it.toModel() }

    fun vibes(): Flow<List<Vibe>> =
        q.activeVibes().asFlow().mapToList(io).mapRows { it.toModel() }

    fun goals(): Flow<List<Goal>> =
        q.goalsWithProgress().asFlow().mapToList(io).mapRows {
            Goal(it.id, it.name, it.target_hours.toInt(), it.achieved != 0L, it.progress.toInt())
        }

    fun entriesFor(date: LocalDate): Flow<List<Entry>> =
        q.entriesForDate(date.toString()).asFlow().mapToList(io).mapRows { it.toModel() }

    suspend fun entryFor(slot: HourSlot): Entry? = withContext(io) {
        q.entryForSlot(slot.date.toString(), slot.hour.toLong()).executeAsOneOrNull()?.toModel()
    }

    suspend fun goalIdsFor(slot: HourSlot): Set<String> = withContext(io) {
        q.goalLogsForSlot(slot.date.toString(), slot.hour.toLong()).executeAsList().map { it.goal_id }.toSet()
    }

    /** The user's most-logged activities over the last 30 days, for the notification's quick buttons. */
    fun topActivities(today: LocalDate, limit: Int = 2): List<Activity> =
        q.topActivitiesSince(today.minus(DatePeriod(days = 30)).toString(), limit.toLong())
            .executeAsList().map { it.toModel() }

    fun activeActivitiesNow(): List<Activity> = q.activeActivities().executeAsList().map { it.toModel() }

    fun outboxCount(): Flow<Long> = q.outboxCount().asFlow().mapToOne(io)

    // ── writes ──────────────────────────────────────────────

    /**
     * Creates or replaces the entry for [slot]. At most one live entry exists per hour.
     * [goalIds], when given, becomes the exact set of goals this hour counts toward.
     */
    suspend fun saveEntry(
        slot: HourSlot,
        activityId: String?,
        vibeId: String?,
        memo: String,
        source: EntrySource,
        categoryGuessed: Boolean = false,
        goalIds: Set<String>? = null,
    ): Entry = withContext(io) {
        val entry = db.transactionWithResult {
            val t = now()
            val trimmed = memo.trim().take(MEMO_MAX_LENGTH)
            val existing = q.entryForSlot(slot.date.toString(), slot.hour.toLong()).executeAsOneOrNull()
            val id = existing?.id ?: Ids.uuidV7(t)
            if (existing == null) {
                q.insertEntry(
                    id, slot.date.toString(), slot.hour.toLong(), activityId, vibeId, trimmed,
                    source.name, if (categoryGuessed) 1 else 0, t, t,
                )
            } else {
                q.updateEntry(activityId, vibeId, trimmed, source.name, if (categoryGuessed) 1 else 0, t, t, id)
            }
            q.enqueue("entry", id, t)
            if (goalIds != null) setGoalsForSlot(slot, goalIds, t)
            q.entryForSlot(slot.date.toString(), slot.hour.toLong()).executeAsOne().toModel()
        }
        onSlotLogged(slot, source)
        entry
    }

    /**
     * Logs [text] for [slot] with no other choices made (notification reply, quick-reply chip,
     * the Log page's send button). The activity is picked automatically.
     */
    suspend fun logText(slot: HourSlot, text: String, source: EntrySource): Entry {
        val activityId = resolveActivity(text)
        val existing = entryFor(slot)
        return saveEntry(
            slot = slot,
            activityId = activityId ?: existing?.activityId,
            vibeId = existing?.vibeId,
            memo = text,
            source = source,
            categoryGuessed = activityId != null,
        )
    }

    /**
     * Saves from the Log page. Activity, vibe and goals are all optional; with no activity chosen,
     * one is picked from the text the same way as a notification reply.
     */
    suspend fun saveFromApp(
        slot: HourSlot,
        activityId: String?,
        vibeId: String?,
        memo: String,
        goalIds: Set<String>,
        source: EntrySource,
    ): Entry {
        val guessed = if (activityId == null && memo.isNotBlank()) resolveActivity(memo) else null
        return saveEntry(
            slot = slot,
            activityId = activityId ?: guessed,
            vibeId = vibeId,
            memo = memo,
            source = source,
            categoryGuessed = guessed != null,
            goalIds = goalIds,
        )
    }

    /**
     * Picks an activity for free text: if the same text was logged before (ignoring case), reuse that
     * entry's activity, as long as it still exists; otherwise guess from keywords. Null = Uncategorized.
     */
    suspend fun resolveActivity(text: String): String? = withContext(io) {
        val active = activeActivitiesNow()
        val activeIds = active.map { it.id }.toSet()
        val key = text.trim().lowercase()
        val reused = q.recentMemos(LocalDate(1970, 1, 1).toString()).executeAsList()
            .firstOrNull { it.memo.trim().lowercase() == key && it.activity_id in activeIds }
            ?.activity_id
        reused ?: CategoryGuesser(active).guess(text)?.id
    }

    /** Recent replies to offer as one-tap suggestions, most used first. */
    fun quickReplies(today: LocalDate, limit: Int = 6): Flow<List<QuickReply>> =
        q.recentMemos(today.minus(DatePeriod(days = 30)).toString()).asFlow().mapToList(io)
            .map { rows -> buildQuickReplies(rows.map { it.memo to it.activity_id }, limit) }

    /** Synchronous version for building notifications. */
    fun quickRepliesNow(today: LocalDate, limit: Int = 3): List<QuickReply> =
        buildQuickReplies(
            q.recentMemos(today.minus(DatePeriod(days = 30)).toString()).executeAsList().map { it.memo to it.activity_id },
            limit,
        )

    /** One-tap logging from a notification button: keeps any memo already written. */
    suspend fun logActivity(slot: HourSlot, activityId: String, source: EntrySource): Entry {
        val existing = entryFor(slot)
        return saveEntry(slot, activityId, existing?.vibeId, existing?.memo.orEmpty(), source)
    }

    suspend fun deleteEntry(id: String) = withContext(io) {
        db.transaction {
            val t = now()
            q.softDeleteEntry(t, id)
            q.enqueue("entry", id, t)
        }
    }

    suspend fun addActivity(name: String, icon: String): Activity = withContext(io) {
        db.transactionWithResult {
            val t = now()
            val id = Ids.uuidV7(t)
            val sort = q.maxActivitySort().executeAsOne() + 1
            q.insertActivity(id, name.trim(), null, icon, "", sort, 0, t)
            q.enqueue("activity", id, t)
            Activity(id, name.trim(), null, icon, emptyList(), sort.toInt(), isDefault = false, archived = false)
        }
    }

    /** Renames an activity and/or changes its icon. Past entries show the new name and icon. */
    suspend fun updateActivity(id: String, name: String, icon: String) = withContext(io) {
        db.transaction {
            val t = now()
            q.updateActivity(name.trim(), icon, t, id)
            q.enqueue("activity", id, t)
        }
    }

    /**
     * Removes an activity from the picker. The row is kept (archived), so entries already logged
     * with it keep showing its name and icon.
     */
    suspend fun deleteActivity(id: String) = withContext(io) {
        db.transaction {
            val t = now()
            q.archiveActivity(t, id)
            q.enqueue("activity", id, t)
        }
    }

    suspend fun addGoal(name: String, targetHours: Int): String = withContext(io) {
        db.transactionWithResult {
            val t = now()
            val id = Ids.uuidV7(t)
            q.insertGoal(id, name.trim(), targetHours.toLong(), q.maxGoalSort().executeAsOne() + 1, t)
            q.enqueue("goal", id, t)
            id
        }
    }

    suspend fun setGoalAchieved(id: String, achieved: Boolean) = withContext(io) {
        db.transaction {
            val t = now()
            q.setGoalAchieved(if (achieved) 1 else 0, t, id)
            q.enqueue("goal", id, t)
        }
    }

    private fun setGoalsForSlot(slot: HourSlot, goalIds: Set<String>, t: Long) {
        val date = slot.date.toString()
        val hour = slot.hour.toLong()
        val current = q.goalLogsForSlot(date, hour).executeAsList()
        current.filter { it.goal_id !in goalIds }.forEach {
            q.softDeleteGoalLog(t, it.id)
            q.enqueue("goal_log", it.id, t)
        }
        val currentIds = current.map { it.goal_id }.toSet()
        (goalIds - currentIds).forEach { goalId ->
            val old = q.goalLogFor(goalId, date, hour).executeAsOneOrNull()
            val id = if (old != null) {
                q.restoreGoalLog(t, old.id); old.id
            } else {
                Ids.uuidV7(t).also { q.insertGoalLog(it, goalId, date, hour, t) }
            }
            q.enqueue("goal_log", id, t)
        }
    }
}

private fun ActivityRow.toModel() = Activity(
    id = id,
    name = name,
    nameKey = name_key,
    icon = icon,
    keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    sortOrder = sort_order.toInt(),
    isDefault = is_default != 0L,
    archived = archived != 0L,
)

private fun VibeRow.toModel() = Vibe(id, label, label_key, kaomoji, sort_order.toInt(), archived != 0L)

private fun EntryRow.toModel() = Entry(
    id = id,
    slot = HourSlot(LocalDate.parse(local_date), hour.toInt()),
    activityId = activity_id,
    vibeId = vibe_id,
    memo = memo,
    source = EntrySource.valueOf(source),
    categoryGuessed = category_guessed != 0L,
    loggedAtMillis = logged_at,
)
