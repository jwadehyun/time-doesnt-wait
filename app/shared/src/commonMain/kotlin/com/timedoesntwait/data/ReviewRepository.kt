package com.timedoesntwait.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.DailyReview
import com.timedoesntwait.domain.Ids
import com.timedoesntwait.domain.RATING_NOTE_MAX_LENGTH
import com.timedoesntwait.domain.REVIEW_MEMO_MAX_LENGTH
import com.timedoesntwait.domain.RatingDimension
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.domain.RatingScore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import com.timedoesntwait.db.Rating_dimension as DimensionRow

/** Daily reviews and the rating dimensions they use. Every write also queues the row for sync. */
class ReviewRepository(
    private val db: AppDatabase,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
) {
    private val q = db.journalQueries

    /** Called after a day's review is saved (e.g. to clear the evening reminder). */
    var onReviewSaved: (LocalDate) -> Unit = {}

    private fun now() = clock.now().toEpochMilliseconds()

    // ── rating dimensions ───────────────────────────────────

    /** Every rating, including deleted ones (they stay on old reviews). Filter on [RatingDimension.archived]. */
    fun ratingDimensions(): Flow<List<RatingDimension>> =
        q.allRatingDimensions().asFlow().mapToList(io).mapRows { it.toModel() }

    suspend fun addRatingDimension(name: String, kind: RatingKind): RatingDimension = withContext(io) {
        db.transactionWithResult {
            val t = now()
            val id = Ids.uuidV7(t)
            val sort = q.maxRatingSort().executeAsOne() + 1
            q.insertRatingDimension(id, name.trim(), null, kind.name, sort, 0, t)
            q.enqueue("rating_dimension", id, t)
            RatingDimension(id, name.trim(), null, kind, sort.toInt(), isDefault = false, archived = false)
        }
    }

    /** Renames a rating or changes its style. Old scores are kept (shown capped to the new range). */
    suspend fun updateRatingDimension(id: String, name: String, kind: RatingKind) = withContext(io) {
        db.transaction {
            val t = now()
            q.updateRatingDimension(name.trim(), kind.name, t, id)
            q.enqueue("rating_dimension", id, t)
        }
    }

    /** Hides a rating from new reviews. Reviews that already scored it keep the score. */
    suspend fun deleteRatingDimension(id: String) = withContext(io) {
        db.transaction {
            val t = now()
            q.archiveRatingDimension(t, id)
            q.enqueue("rating_dimension", id, t)
        }
    }

    // ── reviews ─────────────────────────────────────────────

    suspend fun reviewFor(date: LocalDate): DailyReview? = withContext(io) { load(date) }

    /** Synchronous check for the reminder receiver. */
    fun isReviewed(date: LocalDate): Boolean = q.reviewForDate(date.toString()).executeAsOneOrNull() != null

    /**
     * Creates or updates the review for [date]. [scores] becomes the exact set of optional ratings:
     * a dimension left out is cleared.
     */
    suspend fun saveReview(
        date: LocalDate,
        overall: Int,
        memo: String,
        scores: Map<String, RatingScore>,
    ): DailyReview = withContext(io) {
        require(overall in 1..5) { "overall rating must be 1..5, was $overall" }
        val review = db.transactionWithResult {
            val t = now()
            val trimmedMemo = memo.trim().take(REVIEW_MEMO_MAX_LENGTH)
            val existing = q.reviewForDate(date.toString()).executeAsOneOrNull()
            val id = existing?.id ?: Ids.uuidV7(t)
            if (existing == null) {
                q.insertReview(id, date.toString(), overall.toLong(), trimmedMemo, t, t)
            } else {
                q.updateReview(overall.toLong(), trimmedMemo, t, id)
            }
            q.enqueue("daily_review", id, t)

            val rows = q.reviewRatingsFor(id).executeAsList()
            scores.forEach { (dimensionId, score) ->
                val note = score.note.trim().take(RATING_NOTE_MAX_LENGTH).ifEmpty { null }
                val row = rows.firstOrNull { it.dimension_id == dimensionId }
                val rowId = if (row != null) {
                    q.updateReviewRating(score.value.toLong(), note, t, row.id); row.id
                } else {
                    Ids.uuidV7(t).also { q.insertReviewRating(it, id, dimensionId, score.value.toLong(), note, t) }
                }
                q.enqueue("review_rating", rowId, t)
            }
            rows.filter { it.deleted_at == null && it.dimension_id !in scores }.forEach {
                q.softDeleteReviewRating(t, it.id)
                q.enqueue("review_rating", it.id, t)
            }
            load(date)!!
        }
        onReviewSaved(date)
        review
    }

    /** Removes the day's review and its scores. */
    suspend fun deleteReview(date: LocalDate) = withContext(io) {
        db.transaction {
            val t = now()
            val review = q.reviewForDate(date.toString()).executeAsOneOrNull() ?: return@transaction
            q.softDeleteReview(t, review.id)
            q.enqueue("daily_review", review.id, t)
            q.reviewRatingsFor(review.id).executeAsList().filter { it.deleted_at == null }.forEach {
                q.softDeleteReviewRating(t, it.id)
                q.enqueue("review_rating", it.id, t)
            }
        }
    }

    private fun load(date: LocalDate): DailyReview? {
        val row = q.reviewForDate(date.toString()).executeAsOneOrNull() ?: return null
        val scores = q.reviewRatingsFor(row.id).executeAsList()
            .filter { it.deleted_at == null }
            .associate { it.dimension_id to RatingScore(it.value_.toInt(), it.note.orEmpty()) }
        return DailyReview(row.id, date, row.overall_rating.toInt(), row.memo, row.saved_at, scores)
    }
}

private fun DimensionRow.toModel() = RatingDimension(
    id = id,
    name = name,
    nameKey = name_key,
    kind = RatingKind.parse(kind),
    sortOrder = sort_order.toInt(),
    isDefault = is_default != 0L,
    archived = archived != 0L,
)
