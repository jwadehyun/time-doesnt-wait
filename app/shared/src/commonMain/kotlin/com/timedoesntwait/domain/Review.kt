package com.timedoesntwait.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

/** How a rating is drawn and its range: 1–5 stars, 1–5 hearts or 1–10 segments. */
enum class RatingKind(val max: Int) {
    STARS_5(5), HEARTS_5(5), SEGMENTS_10(10);

    companion object {
        fun parse(value: String): RatingKind = entries.firstOrNull { it.name == value } ?: STARS_5
    }
}

/** One optional rating on the Daily Review, e.g. Productivity (stars) or a custom "Sleep". */
data class RatingDimension(
    val id: String,
    /** User-typed name; for built-in ratings this is the English fallback. */
    val name: String,
    val nameKey: String?,
    val kind: RatingKind,
    val sortOrder: Int,
    val isDefault: Boolean,
    /** Deleted by the user: hidden from new reviews, kept on the reviews that scored it. */
    val archived: Boolean,
)

/** A built-in rating keeps its translated name until the user renames it. */
val RatingDimension.usesBuiltInName: Boolean
    get() = nameKey != null && BuiltIns.RATINGS.any { it.first == nameKey && it.second == name }

data class RatingScore(val value: Int, val note: String = "")

data class DailyReview(
    val id: String,
    val date: LocalDate,
    /** Required, 1–5. */
    val overall: Int,
    val memo: String,
    val savedAtMillis: Long,
    /** Scores by dimension id. Dimensions left blank are absent. */
    val scores: Map<String, RatingScore>,
)

const val REVIEW_MEMO_MAX_LENGTH = 300
const val RATING_NOTE_MAX_LENGTH = 60

/**
 * The day the Review tab opens on. Until 4 AM, an unreviewed yesterday comes first, so a review
 * written just after midnight still counts for the day it describes.
 */
fun defaultReviewDate(now: LocalDateTime, yesterdayReviewed: Boolean): LocalDate =
    if (now.hour < LATE_NIGHT_END_HOUR && !yesterdayReviewed) now.date.minus(DatePeriod(days = 1)) else now.date

const val LATE_NIGHT_END_HOUR = 4
