package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import kotlinx.datetime.LocalDate
import com.timedoesntwait.resources.tl_just_saved
import com.timedoesntwait.resources.unknown
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.domain.BuiltIns
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.domain.CheckinSchedule
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.TimelineRow
import com.timedoesntwait.domain.buildTimeline
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.tl_empty
import com.timedoesntwait.resources.tl_fill_in
import com.timedoesntwait.resources.tl_guessed
import com.timedoesntwait.resources.tl_logged
import com.timedoesntwait.resources.tl_not_logged
import com.timedoesntwait.resources.tl_source_reply
import com.timedoesntwait.resources.tl_today
import com.timedoesntwait.resources.uncategorized
import com.timedoesntwait.ui.components.IconSquareButton
import com.timedoesntwait.ui.components.InkDivider
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.dashedBorder
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.rememberNow
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun TimelineScreen(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    focus: HourSlot?,
    onOpenSlot: (HourSlot) -> Unit,
) {
    val repo = koinInject<JournalRepository>()
    val scheduleRepo = koinInject<ScheduleRepository>()
    val lang = CobaltTheme.language
    val now by rememberNow()
    val day = date ?: now.date

    val entries by remember(day) { repo.entriesFor(day) }.collectAsState(emptyList())
    // Includes deleted (archived) activities, so past entries keep their name and icon.
    val activities by remember { repo.allActivities() }.collectAsState(emptyList())
    val vibes by remember { repo.allVibes() }.collectAsState(emptyList())
    val schedule by remember { scheduleRepo.schedule() }.collectAsState(CheckinSchedule.DEFAULT)
    val listState = rememberLazyListState()

    val rows = buildTimeline(schedule, entries, activities.associateBy { it.id }, vibes.associateBy { it.id }, day, now)
    val expected = schedule.expectedSlots(day).size

    // After a save, scroll to the hour that was just logged.
    val focusIndex = rows.indexOfFirst { it.slot == focus && it is TimelineRow.Logged }
    LaunchedEffect(focus, focusIndex) {
        if (focusIndex >= 0) listState.animateScrollToItem(focusIndex)
    }
    val loggedCount = entries.size

    Column(Modifier.fillMaxSize()) {
        // Date picker strip
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquareButton("left", "-1", { onDateChange(day.minus(DatePeriod(days = 1))) })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelText(TimeLabels.date(day, lang).uppercase(), style = CobaltTheme.type.title)
                PixelText(stringResource(Res.string.tl_logged, loggedCount, expected), style = CobaltTheme.type.small)
            }
            if (day < now.date) {
                IconSquareButton("right", "+1", { onDateChange(day.plus(DatePeriod(days = 1))) })
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
        if (day != now.date) {
            PixelTextButton(
                stringResource(Res.string.tl_today),
                { onDateChange(now.date) },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        // Progress bar: logged / expected
        Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(12.dp).inkBorder(1.dp)) {
            val fraction = if (expected == 0) 0f else (loggedCount.toFloat() / expected).coerceIn(0f, 1f)
            if (fraction > 0f) Box(Modifier.weight(fraction).height(12.dp).background(Cobalt.Ink))
            if (fraction < 1f) Spacer(Modifier.weight(1f - fraction))
        }
        Spacer(Modifier.height(4.dp))
        InkDivider()

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                PixelText(stringResource(Res.string.tl_empty), style = CobaltTheme.type.body)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(rows, key = { "${it.slot.date}-${it.slot.hour}" }) { row ->
                    when (row) {
                        is TimelineRow.Logged -> LoggedRow(row, justSaved = row.slot == focus, onClick = { onOpenSlot(row.slot) })
                        is TimelineRow.Missed -> MissedRow(row.slot, onFill = { onOpenSlot(row.slot) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggedRow(row: TimelineRow.Logged, justSaved: Boolean, onClick: () -> Unit) {
    val lang = CobaltTheme.language
    Row(Modifier.fillMaxWidth()) {
        // Rail with the activity stamp
        Box(
            Modifier.size(40.dp).hardShadow(Cobalt.ShadowSmall).background(Cobalt.Ink),
            contentAlignment = Alignment.Center,
        ) { ActivityIcon(row.activity?.icon ?: BuiltIns.UNKNOWN_ICON, size = 22.dp, color = Cobalt.Card) }
        Spacer(Modifier.width(12.dp))
        PixelCard(
            Modifier.weight(1f).clickable(onClick = onClick),
            padding = 12.dp,
            shadow = if (justSaved) Cobalt.Shadow + 2.dp else Cobalt.ShadowSmall,
        ) {
            if (justSaved) {
                PixelText(stringResource(Res.string.tl_just_saved), style = CobaltTheme.type.label)
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelText(TimeLabels.hourRange(row.slot.hour, lang), style = CobaltTheme.type.label, modifier = Modifier.weight(1f))
                PixelText(
                    "[ ${
                        when {
                            row.activity != null -> row.activity.label()
                            row.activityMissing -> stringResource(Res.string.unknown)
                            else -> stringResource(Res.string.uncategorized)
                        }.uppercase()
                    } ]",
                    style = CobaltTheme.type.small,
                )
            }
            if (row.entry.memo.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                PixelText(row.entry.memo, style = CobaltTheme.type.user)
            }
            val meta = buildList {
                row.vibe?.let { add(it.kaomoji) }
                if (row.entry.categoryGuessed) add(stringResource(Res.string.tl_guessed))
                if (row.entry.source == EntrySource.NOTIFICATION_REPLY || row.entry.source == EntrySource.QUICK_BUTTON) {
                    add(stringResource(Res.string.tl_source_reply))
                }
            }
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                PixelText(meta.joinToString("  ·  "), style = CobaltTheme.type.small)
            }
        }
    }
}

@Composable
private fun MissedRow(slot: HourSlot, onFill: () -> Unit) {
    val lang = CobaltTheme.language
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).dashedBorder(), contentAlignment = Alignment.Center) {
            PixelIcon("question", size = 18.dp)
        }
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier.weight(1f).dashedBorder().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                PixelText(TimeLabels.hourRange(slot.hour, lang), style = CobaltTheme.type.label)
                PixelText(stringResource(Res.string.tl_not_logged), style = CobaltTheme.type.small)
            }
            PixelTextButton(stringResource(Res.string.tl_fill_in), onFill)
        }
    }
}
