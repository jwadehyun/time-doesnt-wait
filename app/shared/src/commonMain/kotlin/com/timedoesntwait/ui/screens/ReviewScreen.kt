package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.ReviewRepository
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.BuiltIns
import com.timedoesntwait.domain.CheckinSchedule
import com.timedoesntwait.domain.DailyReview
import com.timedoesntwait.domain.LATE_NIGHT_END_HOUR
import com.timedoesntwait.domain.RATING_NOTE_MAX_LENGTH
import com.timedoesntwait.domain.REVIEW_MEMO_MAX_LENGTH
import com.timedoesntwait.domain.RatingDimension
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.domain.RatingScore
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.defaultReviewDate
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.ql_edit
import com.timedoesntwait.resources.ql_edit_done
import com.timedoesntwait.resources.ql_delete_confirm
import com.timedoesntwait.resources.rv_add_rating
import com.timedoesntwait.resources.rv_delete
import com.timedoesntwait.resources.rv_deleted_tag
import com.timedoesntwait.resources.rv_hours_logged
import com.timedoesntwait.resources.rv_hours_value
import com.timedoesntwait.resources.rv_memo
import com.timedoesntwait.resources.rv_memo_hint
import com.timedoesntwait.resources.rv_more
import com.timedoesntwait.resources.rv_need_overall
import com.timedoesntwait.resources.rv_no_logs
import com.timedoesntwait.resources.rv_not_reviewed
import com.timedoesntwait.resources.rv_note_hint
import com.timedoesntwait.resources.rv_optional
import com.timedoesntwait.resources.rv_overall
import com.timedoesntwait.resources.rv_overall_1
import com.timedoesntwait.resources.rv_overall_2
import com.timedoesntwait.resources.rv_overall_3
import com.timedoesntwait.resources.rv_overall_4
import com.timedoesntwait.resources.rv_overall_5
import com.timedoesntwait.resources.rv_overall_hint
import com.timedoesntwait.resources.rv_required
import com.timedoesntwait.resources.rv_reviewed
import com.timedoesntwait.resources.rv_save
import com.timedoesntwait.resources.rv_saved
import com.timedoesntwait.resources.rv_score
import com.timedoesntwait.resources.rv_tap_again
import com.timedoesntwait.resources.rv_top_activities
import com.timedoesntwait.resources.rv_update
import com.timedoesntwait.resources.rv_view_timeline
import com.timedoesntwait.resources.st_hours_short
import com.timedoesntwait.resources.uncategorized
import com.timedoesntwait.resources.unknown
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.PixelTextField
import com.timedoesntwait.ui.components.RatingGauge
import com.timedoesntwait.ui.components.SectionCard
import com.timedoesntwait.ui.components.StepperHeader
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.rememberNow
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The Review tab: rate the day 1–5 (required), optional ratings with notes, a bedtime memo.
 * Any day can be reviewed again later; saving replaces that day's review.
 */
@Composable
fun ReviewScreen(
    requestedDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    onOpenTimeline: (LocalDate) -> Unit,
) {
    val reviews = koinInject<ReviewRepository>()
    val now by rememberNow()

    // Until 4 AM an unreviewed yesterday comes first. Decided once per opening, not on every save.
    var defaultDate by remember { mutableStateOf<LocalDate?>(null) }
    val lateNight = now.hour < LATE_NIGHT_END_HOUR
    LaunchedEffect(now.date, lateNight) {
        val yesterday = now.date.minus(DatePeriod(days = 1))
        defaultDate = defaultReviewDate(now, yesterdayReviewed = reviews.reviewFor(yesterday) != null)
    }
    val date = requestedDate ?: defaultDate ?: return
    ReviewDay(date, today = now.date, onDateChange, onOpenTimeline)
}

@Composable
private fun ReviewDay(
    date: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onOpenTimeline: (LocalDate) -> Unit,
) {
    val reviews = koinInject<ReviewRepository>()
    val journal = koinInject<JournalRepository>()
    val scheduleRepo = koinInject<ScheduleRepository>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language

    val dimensions by remember { reviews.ratingDimensions() }.collectAsState(emptyList())
    val entries by remember(date) { journal.entriesFor(date) }.collectAsState(emptyList())
    val activities by remember { journal.allActivities() }.collectAsState(emptyList())
    val schedule by remember { scheduleRepo.schedule() }.collectAsState(CheckinSchedule.DEFAULT)

    var saved by remember(date) { mutableStateOf<DailyReview?>(null) }
    var overall by remember(date) { mutableStateOf<Int?>(null) }
    var scores by remember(date) { mutableStateOf(emptyMap<String, RatingScore>()) }
    var memo by remember(date) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var justSaved by remember(date) { mutableStateOf(false) }
    var confirmDelete by remember(date) { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RatingDimension?>(null) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        val review = reviews.reviewFor(date)
        saved = review
        overall = review?.overall
        scores = review?.scores.orEmpty()
        memo = review?.memo.orEmpty()
    }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2500)
            justSaved = false
        }
    }

    // Deleted ratings stay visible on reviews that scored them.
    val shown = dimensions.filter { !it.archived || it.id in scores || saved?.scores?.containsKey(it.id) == true }

    fun save() {
        val rating = overall ?: return
        if (saving) return
        saving = true
        scope.launch {
            try {
                saved = reviews.saveReview(date, rating, memo, scores)
                confirmDelete = false
                justSaved = true
            } finally {
                saving = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepperHeader(
            title = TimeLabels.date(date, lang).uppercase(),
            subtitle = stringResource(if (saved != null) Res.string.rv_reviewed else Res.string.rv_not_reviewed),
            onPrevious = { onDateChange(date.minus(DatePeriod(days = 1))) },
            onNext = { if (date < today) onDateChange(date.plus(DatePeriod(days = 1))) },
            nextEnabled = date < today,
        )

        DaySummary(
            logged = entries.size,
            expected = schedule.expectedSlots(date).size,
            top = entries.groupingBy { it.activityId }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String?, Int>> { it.value }.thenBy { it.key == null })
                .take(3)
                .map { (id, hours) -> Triple(id, activities.firstOrNull { it.id == id }, hours) },
            onOpenTimeline = { onOpenTimeline(date) },
        )

        // Overall: the one required rating.
        SectionCard(stringResource(Res.string.rv_overall)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelText(
                        overall?.let { stringResource(OVERALL_LABELS[it - 1]) } ?: stringResource(Res.string.rv_overall_hint),
                        style = if (overall != null) CobaltTheme.type.title else CobaltTheme.type.body,
                        modifier = Modifier.weight(1f),
                    )
                    Tag(stringResource(Res.string.rv_required))
                }
                Spacer(Modifier.height(8.dp))
                RatingGauge(RatingKind.STARS_5, overall, onChange = { overall = it }, iconSize = 36.dp)
            }
        }

        // Optional ratings, editable like activities on the Log page.
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelText(stringResource(Res.string.rv_more), style = CobaltTheme.type.label)
                Spacer(Modifier.width(8.dp))
                Tag(stringResource(Res.string.rv_optional))
                Spacer(Modifier.weight(1f))
                PixelText(
                    stringResource(if (editMode) Res.string.ql_edit_done else Res.string.ql_edit),
                    style = CobaltTheme.type.label,
                    modifier = Modifier.clickable(role = Role.Button) { editMode = !editMode }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
            PixelText(stringResource(Res.string.rv_tap_again), style = CobaltTheme.type.small)
        }
        shown.forEach { dimension ->
            RatingCard(
                dimension = dimension,
                score = scores[dimension.id],
                editMode = editMode,
                onEdit = { editing = dimension },
                onScore = { value ->
                    scores = if (value == null) scores - dimension.id
                    else scores + (dimension.id to RatingScore(value, scores[dimension.id]?.note.orEmpty()))
                },
                onNote = { note ->
                    scores[dimension.id]?.let { scores = scores + (dimension.id to it.copy(note = note)) }
                },
            )
        }
        PixelTextButton(stringResource(Res.string.rv_add_rating), { adding = true }, Modifier.fillMaxWidth())

        SectionCard(stringResource(Res.string.rv_memo)) {
            PixelTextField(
                memo,
                { memo = it },
                stringResource(Res.string.rv_memo_hint),
                modifier = Modifier.padding(12.dp),
                maxLength = REVIEW_MEMO_MAX_LENGTH,
            )
        }

        Column(Modifier.fillMaxWidth().halftone(Cobalt.Ink.copy(alpha = 0.35f)).padding(vertical = 12.dp)) {
            PixelTextButton(
                stringResource(if (saved == null) Res.string.rv_save else Res.string.rv_update),
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                big = true,
                enabled = overall != null && !saving,
            )
        }
        when {
            justSaved -> PixelText(stringResource(Res.string.rv_saved), style = CobaltTheme.type.label)
            overall == null -> PixelText(stringResource(Res.string.rv_need_overall), style = CobaltTheme.type.small)
        }

        if (saved != null) {
            PixelTextButton(
                stringResource(if (confirmDelete) Res.string.ql_delete_confirm else Res.string.rv_delete),
                {
                    if (!confirmDelete) {
                        confirmDelete = true
                    } else {
                        scope.launch {
                            reviews.deleteReview(date)
                            saved = null
                            overall = null
                            scores = emptyMap()
                            memo = ""
                            confirmDelete = false
                        }
                    }
                },
                Modifier.fillMaxWidth(),
                danger = true,
                filled = confirmDelete,
            )
        }
    }

    if (adding) {
        RatingEditorDialog(
            existing = null,
            onDismiss = { adding = false },
            onSave = { name, kind ->
                scope.launch {
                    reviews.addRatingDimension(name, kind)
                    adding = false
                }
            },
            onDelete = null,
        )
    }
    editing?.let { dimension ->
        RatingEditorDialog(
            existing = dimension,
            onDismiss = { editing = null },
            onSave = { name, kind ->
                scope.launch {
                    reviews.updateRatingDimension(dimension.id, name, kind)
                    // A score above the new range is capped, the same way it is shown.
                    scores[dimension.id]?.let { s ->
                        if (s.value > kind.max) scores = scores + (dimension.id to s.copy(value = kind.max))
                    }
                    editing = null
                }
            },
            onDelete = {
                scope.launch {
                    reviews.deleteRatingDimension(dimension.id)
                    editing = null
                }
            },
        )
    }
}

private val OVERALL_LABELS = listOf(
    Res.string.rv_overall_1, Res.string.rv_overall_2, Res.string.rv_overall_3,
    Res.string.rv_overall_4, Res.string.rv_overall_5,
)

@Composable
private fun DaySummary(
    logged: Int,
    expected: Int,
    top: List<Triple<String?, Activity?, Int>>,
    onOpenTimeline: () -> Unit,
) {
    PixelCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                PixelText(stringResource(Res.string.rv_hours_logged), style = CobaltTheme.type.small)
                PixelText(stringResource(Res.string.rv_hours_value, logged, expected), style = CobaltTheme.type.display)
            }
            PixelText(
                stringResource(Res.string.rv_view_timeline),
                style = CobaltTheme.type.label,
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenTimeline).padding(vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (top.isEmpty()) {
            PixelText(stringResource(Res.string.rv_no_logs), style = CobaltTheme.type.small)
        } else {
            PixelText(stringResource(Res.string.rv_top_activities), style = CobaltTheme.type.small)
            Spacer(Modifier.height(6.dp))
            top.forEach { (id, activity, hours) ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActivityIcon(activity?.icon ?: BuiltIns.UNKNOWN_ICON, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    PixelText(
                        activity?.label() ?: stringResource(if (id == null) Res.string.uncategorized else Res.string.unknown),
                        style = CobaltTheme.type.label,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    PixelText(stringResource(Res.string.st_hours_short, hours), style = CobaltTheme.type.label)
                }
            }
        }
    }
}

@Composable
private fun RatingCard(
    dimension: RatingDimension,
    score: RatingScore?,
    editMode: Boolean,
    onEdit: () -> Unit,
    onScore: (Int?) -> Unit,
    onNote: (String) -> Unit,
) {
    PixelCard(Modifier.fillMaxWidth(), padding = 12.dp, shadow = Cobalt.ShadowSmall) {
        Row(
            Modifier.fillMaxWidth().then(if (editMode) Modifier.clickable(role = Role.Button, onClick = onEdit) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelText(dimension.label().uppercase(), style = CobaltTheme.type.label, modifier = Modifier.weight(1f), maxLines = 1)
            if (dimension.archived) {
                Tag(stringResource(Res.string.rv_deleted_tag))
                Spacer(Modifier.width(8.dp))
            }
            PixelText(
                score?.let { stringResource(Res.string.rv_score, it.value.coerceAtMost(dimension.kind.max), dimension.kind.max) } ?: "–",
                style = CobaltTheme.type.label,
            )
            if (editMode) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(24.dp).background(Cobalt.Ink), contentAlignment = Alignment.Center) {
                    PixelIcon("pencil", size = 14.dp, color = Cobalt.Card)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        RatingGauge(dimension.kind, score?.value, onChange = onScore, iconSize = 28.dp)
        if (score != null) {
            Spacer(Modifier.height(8.dp))
            PixelTextField(
                score.note,
                onNote,
                stringResource(Res.string.rv_note_hint),
                maxLength = RATING_NOTE_MAX_LENGTH,
                singleLine = true,
            )
        }
    }
}

/** Small outlined label, e.g. REQUIRED / OPTIONAL. */
@Composable
private fun Tag(text: String) {
    PixelText(text, style = CobaltTheme.type.small, modifier = Modifier.inkBorder(1.dp).padding(horizontal = 6.dp, vertical = 2.dp))
}
