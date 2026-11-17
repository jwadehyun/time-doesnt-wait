package com.timedoesntwait.data

import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.domain.RatingScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ReviewRepositoryTest {
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(1_794_640_804_050)
    }
    private lateinit var db: AppDatabase
    private lateinit var reviews: ReviewRepository
    private val day = LocalDate(2026, 11, 16)

    @BeforeTest
    fun setUp() {
        db = AppDatabase(testDriverFactory().create())
        JournalRepository(db, clock, Dispatchers.Unconfined).seedDefaultsIfNeeded()
        reviews = ReviewRepository(db, clock, Dispatchers.Unconfined)
    }

    @Test
    fun seedsThreeDefaultRatings() = runTest {
        val dims = reviews.ratingDimensions().first()
        assertEquals(listOf("productivity", "happiness", "progress"), dims.map { it.nameKey })
        assertEquals(listOf(RatingKind.STARS_5, RatingKind.HEARTS_5, RatingKind.SEGMENTS_10), dims.map { it.kind })
    }

    @Test
    fun saveUpdateAndClearScores() = runTest {
        val (productivity, happiness, progress) = reviews.ratingDimensions().first()
        var savedFor: LocalDate? = null
        reviews.onReviewSaved = { savedFor = it }
        val queuedBefore = db.journalQueries.outboxCount().executeAsOne()

        val first = reviews.saveReview(
            day, overall = 4, memo = "  good day  ",
            scores = mapOf(productivity.id to RatingScore(5, "shipped it"), progress.id to RatingScore(7)),
        )
        assertEquals(day, savedFor)
        assertEquals("good day", first.memo)
        assertEquals(RatingScore(5, "shipped it"), first.scores[productivity.id])
        assertTrue(reviews.isReviewed(day))
        assertFalse(reviews.isReviewed(LocalDate(2026, 11, 15)))

        // Update: change one score, clear another, add a third.
        val second = reviews.saveReview(
            day, overall = 3, memo = "",
            scores = mapOf(productivity.id to RatingScore(2), happiness.id to RatingScore(4, "friends")),
        )
        assertEquals(first.id, second.id, "one review per day")
        assertEquals(3, second.overall)
        assertEquals(setOf(productivity.id, happiness.id), second.scores.keys)
        assertEquals(RatingScore(2, ""), second.scores[productivity.id])

        // Setting a cleared score again reuses its row.
        reviews.saveReview(day, 3, "", mapOf(progress.id to RatingScore(9)))
        val rows = db.journalQueries.reviewRatingsFor(first.id).executeAsList()
        assertEquals(3, rows.size)
        assertEquals(1, rows.count { it.deleted_at == null })
        // 1 review + 3 score rows queued for sync.
        assertEquals(queuedBefore + 4, db.journalQueries.outboxCount().executeAsOne())
    }

    @Test
    fun overallRatingIsRequired() = runTest {
        assertFailsWith<IllegalArgumentException> { reviews.saveReview(day, 0, "", emptyMap()) }
        assertNull(reviews.reviewFor(day))
    }

    @Test
    fun deleteReviewThenReviewAgain() = runTest {
        val dims = reviews.ratingDimensions().first()
        val old = reviews.saveReview(day, 2, "meh", mapOf(dims[0].id to RatingScore(1)))
        reviews.deleteReview(day)
        assertNull(reviews.reviewFor(day))
        assertFalse(reviews.isReviewed(day))
        assertTrue(db.journalQueries.reviewRatingsFor(old.id).executeAsList().all { it.deleted_at != null })

        val again = reviews.saveReview(day, 5, "", emptyMap())
        assertTrue(again.id != old.id)
        assertEquals(5, reviews.reviewFor(day)!!.overall)
    }

    @Test
    fun customRatingsCanBeEditedAndDeletedWithoutLosingOldScores() = runTest {
        val sleep = reviews.addRatingDimension("  Sleep ", RatingKind.HEARTS_5)
        assertEquals("Sleep", sleep.name)
        reviews.saveReview(day, 4, "", mapOf(sleep.id to RatingScore(3)))

        reviews.updateRatingDimension(sleep.id, "Sleep quality", RatingKind.SEGMENTS_10)
        val edited = reviews.ratingDimensions().first().single { it.id == sleep.id }
        assertEquals("Sleep quality", edited.name)
        assertEquals(RatingKind.SEGMENTS_10, edited.kind)

        reviews.deleteRatingDimension(sleep.id)
        val deleted = reviews.ratingDimensions().first().single { it.id == sleep.id }
        assertTrue(deleted.archived, "kept, but hidden from new reviews")
        assertEquals(RatingScore(3), reviews.reviewFor(day)!!.scores[sleep.id], "old score kept")
    }
}
