package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.StatsRepository
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.ActivityShare
import com.timedoesntwait.domain.BuiltIns
import com.timedoesntwait.domain.DateRange
import com.timedoesntwait.domain.DayStat
import com.timedoesntwait.domain.Goal
import com.timedoesntwait.domain.PeriodStats
import com.timedoesntwait.domain.StatsPeriod
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.shift
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.ql_goal_achieved
import com.timedoesntwait.resources.ql_goal_progress
import com.timedoesntwait.resources.st_achieved
import com.timedoesntwait.resources.st_activities
import com.timedoesntwait.resources.st_avg
import com.timedoesntwait.resources.st_avg_none
import com.timedoesntwait.resources.st_best
import com.timedoesntwait.resources.st_daily
import com.timedoesntwait.resources.st_days
import com.timedoesntwait.resources.st_full
import com.timedoesntwait.resources.st_goal_left
import com.timedoesntwait.resources.st_goal_period
import com.timedoesntwait.resources.st_goal_reached
import com.timedoesntwait.resources.st_goals
import com.timedoesntwait.resources.st_hours
import com.timedoesntwait.resources.st_hours_short
import com.timedoesntwait.resources.st_month
import com.timedoesntwait.resources.st_no_goals
import com.timedoesntwait.resources.st_no_hours
import com.timedoesntwait.resources.st_partial
import com.timedoesntwait.resources.st_rated_days
import com.timedoesntwait.resources.st_streak
import com.timedoesntwait.resources.st_tap_day
import com.timedoesntwait.resources.st_vs_month
import com.timedoesntwait.resources.st_vs_week
import com.timedoesntwait.resources.st_week
import com.timedoesntwait.resources.tl_today
import com.timedoesntwait.resources.uncategorized
import com.timedoesntwait.resources.unknown
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelChip
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.SectionCard
import com.timedoesntwait.ui.components.StepperHeader
import com.timedoesntwait.ui.components.dashedBorder
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.components.hatched
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.rememberNow
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.abs

/** The Stats tab: week or month totals, daily bars, activity split and goal progress. */
@Composable
fun StatsScreen(
    period: StatsPeriod,
    anchor: LocalDate?,
    onPeriodChange: (StatsPeriod) -> Unit,
    onAnchorChange: (LocalDate?) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val statsRepo = koinInject<StatsRepository>()
    val journal = koinInject<JournalRepository>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language
    val now by rememberNow()
    val today = now.date

    val range = DateRange.containing(anchor ?: today, period)
    val stats by remember(range, today) { statsRepo.stats(range, period, today) }.collectAsState(null)
    val goalHours by remember(range) { statsRepo.goalHours(range) }.collectAsState(emptyMap())
    val goals by remember { journal.goals() }.collectAsState(emptyList())
    val activities by remember { journal.allActivities() }.collectAsState(emptyList())
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelChip(stringResource(Res.string.st_week), period == StatsPeriod.WEEK, { onPeriodChange(StatsPeriod.WEEK) }, Modifier.weight(1f))
            PixelChip(stringResource(Res.string.st_month), period == StatsPeriod.MONTH, { onPeriodChange(StatsPeriod.MONTH) }, Modifier.weight(1f))
        }
        StepperHeader(
            title = when (period) {
                StatsPeriod.WEEK -> TimeLabels.dayRange(range.start, range.end, lang)
                StatsPeriod.MONTH -> TimeLabels.month(range.start, lang)
            }.uppercase(),
            subtitle = null,
            onPrevious = { onAnchorChange(range.shift(period, -1).start) },
            onNext = {
                val next = range.shift(period, 1)
                if (next.start <= today) onAnchorChange(if (today in next) null else next.start)
            },
            nextEnabled = range.end < today,
        )

        val s = stats ?: return@Column
        Tiles(s, period)

        SectionCard(stringResource(Res.string.st_daily)) {
            Column(Modifier.padding(12.dp)) {
                when (period) {
                    StatsPeriod.WEEK -> WeekBars(s.days, onOpenDay)
                    StatsPeriod.MONTH -> MonthGrid(s.days, onOpenDay)
                }
                Spacer(Modifier.height(12.dp))
                Legend()
                Spacer(Modifier.height(6.dp))
                PixelText(stringResource(Res.string.st_tap_day), style = CobaltTheme.type.small)
            }
        }

        SectionCard(stringResource(Res.string.st_activities)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (s.activities.isEmpty()) {
                    PixelText(stringResource(Res.string.st_no_hours), style = CobaltTheme.type.small)
                }
                val byId = activities.associateBy { it.id }
                s.activities.forEach { share -> ActivityShareRow(share, share.activityId?.let(byId::get)) }
            }
        }

        SectionCard(stringResource(Res.string.st_goals)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (goals.isEmpty()) PixelText(stringResource(Res.string.st_no_goals), style = CobaltTheme.type.small)
                goals.filter { !it.achieved }.forEach { goal ->
                    GoalProgress(goal, goalHours[goal.id] ?: 0) { editingGoal = goal }
                }
                val achieved = goals.filter { it.achieved }
                if (achieved.isNotEmpty()) {
                    PixelText(stringResource(Res.string.st_achieved), style = CobaltTheme.type.label)
                    achieved.forEach { goal -> AchievedGoal(goal) { editingGoal = goal } }
                }
            }
        }
    }

    editingGoal?.let { goal ->
        GoalEditorDialog(
            existing = goal,
            onDismiss = { editingGoal = null },
            onSave = { name, target, achieved ->
                scope.launch {
                    journal.updateGoal(goal.id, name, target, achieved)
                    editingGoal = null
                }
            },
            onDelete = {
                scope.launch {
                    journal.deleteGoal(goal.id)
                    editingGoal = null
                }
            },
        )
    }
}

@Composable
private fun Tiles(s: PeriodStats, period: StatsPeriod) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val change = s.hoursChange
        val sign = if (change > 0) "+" else if (change < 0) "−" else "±"
        val changeText = sign + stringResource(Res.string.st_hours_short, abs(change))
        Tile(
            label = stringResource(Res.string.st_hours),
            value = stringResource(Res.string.st_hours_short, s.totalHours),
            sub = stringResource(if (period == StatsPeriod.WEEK) Res.string.st_vs_week else Res.string.st_vs_month, changeText),
            modifier = Modifier.weight(1f),
        )
        Tile(
            label = stringResource(Res.string.st_avg),
            value = s.averageRating?.let { TimeLabels.oneDecimal(it) + "★" } ?: "–",
            sub = if (s.ratedDays == 0) stringResource(Res.string.st_avg_none) else stringResource(Res.string.st_rated_days, s.ratedDays),
            modifier = Modifier.weight(1f),
        )
        Tile(
            label = stringResource(Res.string.st_streak),
            value = stringResource(Res.string.st_days, s.streak.current),
            sub = stringResource(Res.string.st_best, s.streak.best),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Tile(label: String, value: String, sub: String, modifier: Modifier) {
    PixelCard(modifier.fillMaxHeight(), padding = 10.dp, shadow = Cobalt.ShadowSmall) {
        PixelText(label, style = CobaltTheme.type.small, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        PixelText(value, style = CobaltTheme.type.display, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        PixelText(sub, style = CobaltTheme.type.small, maxLines = 2)
    }
}

/** How a day's bar or cell is filled: solid = all scheduled hours logged, hatched = some, dotted = today. */
private fun Modifier.dayFill(day: DayStat): Modifier = when {
    day.isToday -> this.halftone(Cobalt.Ink, spacing = 4.dp)
    day.fullyLogged -> this.background(Cobalt.Ink)
    day.loggedHours > 0 -> this.background(Cobalt.Card).hatched()
    else -> this
}

@Composable
private fun WeekBars(days: List<DayStat>, onOpenDay: (LocalDate) -> Unit) {
    val lang = CobaltTheme.language
    val scale = days.maxOf { maxOf(it.expectedHours, it.loggedHours) }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            Column(
                Modifier.weight(1f)
                    .clickable(enabled = !day.isFuture, role = Role.Button) { onOpenDay(day.date) }
                    .semantics { contentDescription = "${TimeLabels.date(day.date, lang)}: ${day.loggedHours}/${day.expectedHours}" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelText(day.rating?.let { "$it★" } ?: " ", style = CobaltTheme.type.small, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.BottomCenter) {
                    if (!day.isFuture) {
                        val fraction = (day.loggedHours.toFloat() / scale).coerceIn(0f, 1f)
                        if (day.loggedHours > 0) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(fraction).dayFill(day).inkBorder())
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Cobalt.Ink).align(Alignment.BottomCenter))
                }
                Spacer(Modifier.height(4.dp))
                PixelText(if (day.isFuture) "" else day.loggedHours.toString(), style = CobaltTheme.type.label)
                PixelText(
                    TimeLabels.weekdayShort(day.date.dayOfWeek, lang).uppercase(),
                    style = CobaltTheme.type.small,
                    align = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(days: List<DayStat>, onOpenDay: (LocalDate) -> Unit) {
    val lang = CobaltTheme.language
    val leading = days.first().date.dayOfWeek.ordinal // Monday = 0
    val cells: List<DayStat?> = List(leading) { null } + days
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        kotlinx.datetime.DayOfWeek.entries.forEach {
            PixelText(TimeLabels.weekdayShort(it, lang).uppercase(), style = CobaltTheme.type.small, align = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(4.dp))
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.forEach { day ->
                if (day == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    val solid = day.fullyLogged && !day.isToday
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(Cobalt.Card)
                            .dayFill(day)
                            .then(if (day.isToday) Modifier.dashedBorder() else Modifier.inkBorder(color = if (day.isFuture) Cobalt.InkFaint else Cobalt.Ink))
                            .clickable(enabled = !day.isFuture, role = Role.Button) { onOpenDay(day.date) }
                            .semantics { contentDescription = "${TimeLabels.date(day.date, lang)}: ${day.loggedHours}/${day.expectedHours}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        PixelText(
                            day.date.day.toString(),
                            style = CobaltTheme.type.label,
                            color = when {
                                solid -> Cobalt.Card
                                day.isFuture -> Cobalt.InkFaint
                                else -> Cobalt.Ink
                            },
                            modifier = if (!solid && day.loggedHours > 0) Modifier.background(Cobalt.Card).padding(horizontal = 2.dp) else Modifier,
                        )
                        if (day.rating != null) {
                            PixelText(
                                "${day.rating}★",
                                style = CobaltTheme.type.small.copy(fontSize = CobaltTheme.type.small.fontSize * 0.85f),
                                color = if (solid) Cobalt.Card else Cobalt.Ink,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                                    .then(if (solid) Modifier else Modifier.background(Cobalt.Card).padding(horizontal = 2.dp)),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(Modifier.background(Cobalt.Ink), stringResource(Res.string.st_full))
        LegendItem(Modifier.background(Cobalt.Card).hatched(), stringResource(Res.string.st_partial))
        LegendItem(Modifier.halftone(Cobalt.Ink, spacing = 4.dp), stringResource(Res.string.tl_today))
    }
}

@Composable
private fun LegendItem(fill: Modifier, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).then(fill).inkBorder(1.dp))
        Spacer(Modifier.width(4.dp))
        PixelText(label, style = CobaltTheme.type.small)
    }
}

@Composable
private fun ActivityShareRow(share: ActivityShare, activity: Activity?) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActivityIcon(activity?.icon ?: BuiltIns.UNKNOWN_ICON, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            PixelText(
                (activity?.label() ?: stringResource(if (share.activityId == null) Res.string.uncategorized else Res.string.unknown)).uppercase(),
                style = CobaltTheme.type.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            PixelText(
                "${stringResource(Res.string.st_hours_short, share.hours)} · ${(share.fraction * 100).toInt()}%",
                style = CobaltTheme.type.label,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).inkBorder(1.dp)) {
            Box(Modifier.fillMaxWidth(share.fraction.coerceIn(0.02f, 1f)).fillMaxHeight().background(Cobalt.Ink))
        }
    }
}

@Composable
private fun GoalProgress(goal: Goal, periodHours: Int, onClick: () -> Unit) {
    val pct = if (goal.targetHours == 0) 100 else goal.progressHours * 100 / goal.targetHours
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelText(goal.name, style = CobaltTheme.type.user, modifier = Modifier.weight(1f), maxLines = 1)
            PixelText(
                "${stringResource(Res.string.ql_goal_progress, goal.progressHours, goal.targetHours)} ($pct%)",
                style = CobaltTheme.type.small,
            )
        }
        Spacer(Modifier.height(6.dp))
        val segments = 10
        val filled = (pct * segments / 100).coerceIn(0, segments)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(segments) { i ->
                Box(
                    Modifier.weight(1f).height(12.dp).inkBorder(1.dp)
                        .then(if (i < filled) Modifier.background(Cobalt.Ink) else Modifier),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            val left = goal.targetHours - goal.progressHours
            PixelText(
                if (left > 0) stringResource(Res.string.st_goal_left, left) else stringResource(Res.string.st_goal_reached),
                style = CobaltTheme.type.small,
                modifier = Modifier.weight(1f),
            )
            PixelText(stringResource(Res.string.st_goal_period, periodHours), style = CobaltTheme.type.small)
        }
    }
}

@Composable
private fun AchievedGoal(goal: Goal, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelText(goal.name, style = CobaltTheme.type.user, modifier = Modifier.weight(1f), maxLines = 1)
        PixelText(
            "${stringResource(Res.string.ql_goal_progress, goal.progressHours, goal.targetHours)}  ${stringResource(Res.string.ql_goal_achieved)}",
            style = CobaltTheme.type.small,
        )
    }
}
