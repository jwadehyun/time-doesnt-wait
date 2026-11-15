package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.Goal
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.MEMO_MAX_LENGTH
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.Vibe
import com.timedoesntwait.domain.currentSlot
import com.timedoesntwait.domain.pingForSlot
import com.timedoesntwait.platform.PermissionStatus
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.platform.SetupPermission
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.cancel
import com.timedoesntwait.resources.create
import com.timedoesntwait.resources.perm_banner_alarms
import com.timedoesntwait.resources.perm_banner_late
import com.timedoesntwait.resources.perm_banner_notifications
import com.timedoesntwait.resources.ql_activity
import com.timedoesntwait.resources.ql_activity_name
import com.timedoesntwait.resources.ql_add
import com.timedoesntwait.resources.ql_goal_achieved
import com.timedoesntwait.resources.ql_goal_mark_achieved
import com.timedoesntwait.resources.ql_goal_name
import com.timedoesntwait.resources.ql_goal_new
import com.timedoesntwait.resources.ql_goal_plus
import com.timedoesntwait.resources.ql_goal_progress
import com.timedoesntwait.resources.ql_goal_target
import com.timedoesntwait.resources.ql_goals
import com.timedoesntwait.resources.ql_header
import com.timedoesntwait.resources.ql_logged_already
import com.timedoesntwait.resources.ql_memo_hint
import com.timedoesntwait.resources.ql_new_activity
import com.timedoesntwait.resources.ql_new_goal
import com.timedoesntwait.resources.ql_question
import com.timedoesntwait.resources.ql_save
import com.timedoesntwait.resources.ql_saved
import com.timedoesntwait.resources.ql_single_choice
import com.timedoesntwait.resources.ql_vibe
import com.timedoesntwait.ui.components.Banner
import com.timedoesntwait.ui.components.PixelButton
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.PixelTextField
import com.timedoesntwait.ui.components.SectionLabel
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.rememberNow
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun QuickLogScreen(
    requestedSlot: HourSlot?,
    onSlotChange: (HourSlot?) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val repo = koinInject<JournalRepository>()
    val settings = koinInject<SettingsRepository>()
    val platform = koinInject<Platform>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language
    val now by rememberNow()

    val slot = requestedSlot ?: currentSlot(now)
    val activities by repo.activities().collectAsState(emptyList())
    val vibes by repo.vibes().collectAsState(emptyList())
    val goals by repo.goals().collectAsState(emptyList())
    val permissions by platform.permissions.collectAsState()
    val lateness by settings.lastAlarmLatenessFlow().collectAsState(0)

    var activityId by remember { mutableStateOf<String?>(null) }
    var vibeId by remember { mutableStateOf<String?>(null) }
    var memo by remember { mutableStateOf("") }
    var goalIds by remember { mutableStateOf(emptySet<String>()) }
    var alreadyLogged by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var showNewActivity by remember { mutableStateOf(false) }
    var showNewGoal by remember { mutableStateOf(false) }

    LaunchedEffect(slot) {
        val existing = repo.entryFor(slot)
        activityId = existing?.activityId
        vibeId = existing?.vibeId
        memo = existing?.memo.orEmpty()
        goalIds = repo.goalIdsFor(slot)
        alreadyLogged = existing != null
    }
    val savedText = stringResource(Res.string.ql_saved, TimeLabels.hourRange(slot.hour, lang))
    LaunchedEffect(savedMessage) {
        if (savedMessage != null) {
            delay(2500)
            savedMessage = null
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Permission problems first: they decide whether check-ins arrive at all.
        when {
            permissions.of(SetupPermission.NOTIFICATIONS) == PermissionStatus.NEEDED ->
                Banner(stringResource(Res.string.perm_banner_notifications), onOpenPermissions)
            permissions.of(SetupPermission.EXACT_ALARMS) == PermissionStatus.NEEDED ->
                Banner(stringResource(Res.string.perm_banner_alarms), onOpenPermissions)
            lateness > 5 && permissions.of(SetupPermission.BATTERY) == PermissionStatus.NEEDED ->
                Banner(stringResource(Res.string.perm_banner_late, lateness), onOpenPermissions)
        }

        // Header strip: which hour is being logged, with ← → to backfill other hours.
        Row(
            Modifier.fillMaxWidth().hardShadow().background(Cobalt.Ink).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderArrow("left") { onSlotChange(previous(slot)) }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelText(
                    stringResource(Res.string.ql_header, TimeLabels.clock(pingForSlot(slot).hour, lang)),
                    style = CobaltTheme.type.title,
                    color = Cobalt.Card,
                    align = TextAlign.Center,
                )
                PixelText(TimeLabels.date(slot.date, lang).uppercase(), style = CobaltTheme.type.small, color = Cobalt.Card)
            }
            val next = next(slot)
            val canGoNext = pingForSlot(next) <= now
            HeaderArrow("right", enabled = canGoNext) { if (canGoNext) onSlotChange(next) }
        }

        PixelCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            PixelText(stringResource(Res.string.ql_question, TimeLabels.hourRange(slot.hour, lang)), style = CobaltTheme.type.title)
            if (alreadyLogged) {
                Spacer(Modifier.height(6.dp))
                PixelText(stringResource(Res.string.ql_logged_already), style = CobaltTheme.type.small)
            }
        }

        // Activities: single choice, tap again to clear.
        SectionLabel(stringResource(Res.string.ql_activity), trailing = stringResource(Res.string.ql_single_choice))
        ActivityGrid(
            activities = activities,
            selectedId = activityId,
            onSelect = { activityId = if (activityId == it) null else it },
            onAdd = { showNewActivity = true },
        )

        SectionLabel(stringResource(Res.string.ql_vibe))
        VibeGrid(vibes, vibeId) { vibeId = if (vibeId == it) null else it }

        val openGoals = goals.filter { !it.achieved || it.id in goalIds }
        SectionLabel(stringResource(Res.string.ql_goals))
        openGoals.forEach { goal ->
            GoalRow(
                goal = goal,
                countedThisHour = goal.id in goalIds,
                onToggle = { goalIds = if (goal.id in goalIds) goalIds - goal.id else goalIds + goal.id },
                onMarkAchieved = { scope.launch { repo.setGoalAchieved(goal.id, true) } },
            )
        }
        PixelTextButton(stringResource(Res.string.ql_goal_new), { showNewGoal = true }, Modifier.fillMaxWidth())

        PixelTextField(
            value = memo,
            onValueChange = { memo = it },
            placeholder = stringResource(Res.string.ql_memo_hint),
            maxLength = MEMO_MAX_LENGTH,
        )

        if (savedMessage != null) {
            PixelText(savedMessage!!, style = CobaltTheme.type.label, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().halftone(Cobalt.Ink.copy(alpha = 0.35f)).padding(vertical = 12.dp)) {
            PixelTextButton(
                stringResource(Res.string.ql_save),
                onClick = {
                    scope.launch {
                        repo.saveEntry(
                            slot = slot,
                            activityId = activityId,
                            vibeId = vibeId,
                            memo = memo,
                            source = if (pingForSlot(slot) < now.minusHours(1)) EntrySource.BACKFILL else EntrySource.APP,
                            goalIds = goalIds,
                        )
                        alreadyLogged = true
                        savedMessage = savedText
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                big = true,
            )
        }
    }

    if (showNewActivity) {
        NameDialog(
            title = stringResource(Res.string.ql_new_activity),
            fieldHint = stringResource(Res.string.ql_activity_name),
            withNumber = false,
            onDismiss = { showNewActivity = false },
            onCreate = { name, _ ->
                scope.launch {
                    activityId = repo.addActivity(name).id
                    showNewActivity = false
                }
            },
        )
    }
    if (showNewGoal) {
        NameDialog(
            title = stringResource(Res.string.ql_new_goal),
            fieldHint = stringResource(Res.string.ql_goal_name),
            withNumber = true,
            onDismiss = { showNewGoal = false },
            onCreate = { name, hours ->
                scope.launch {
                    repo.addGoal(name, hours)
                    showNewGoal = false
                }
            },
        )
    }
}

private fun previous(slot: HourSlot) =
    if (slot.hour == 0) HourSlot(slot.date.minus(DatePeriod(days = 1)), 23) else HourSlot(slot.date, slot.hour - 1)

private fun next(slot: HourSlot) =
    if (slot.hour == 23) HourSlot(slot.date.plus(DatePeriod(days = 1)), 0) else HourSlot(slot.date, slot.hour + 1)

private fun kotlinx.datetime.LocalDateTime.minusHours(h: Int): kotlinx.datetime.LocalDateTime {
    val total = hour - h
    return if (total >= 0) kotlinx.datetime.LocalDateTime(date, kotlinx.datetime.LocalTime(total, minute))
    else kotlinx.datetime.LocalDateTime(date.minus(DatePeriod(days = 1)), kotlinx.datetime.LocalTime(total + 24, minute))
}

@Composable
private fun HeaderArrow(icon: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.width(44.dp).height(44.dp).inkBorder(color = if (enabled) Cobalt.Card else Cobalt.Ink)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { PixelIcon(icon, size = 18.dp, color = if (enabled) Cobalt.Card else Cobalt.DeepInk) }
}

@Composable
private fun ActivityGrid(activities: List<Activity>, selectedId: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    val tiles: List<Activity?> = activities + null // null = the ADD tile
    tiles.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 4.dp)) {
            row.forEach { activity ->
                val selected = activity != null && activity.id == selectedId
                PixelButton(
                    onClick = { if (activity == null) onAdd() else onSelect(activity.id) },
                    filled = selected,
                    modifier = Modifier.weight(1f).height(72.dp),
                    contentPadding = PaddingValues(4.dp),
                ) { fg ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PixelIcon(activity?.icon ?: "plus", size = 24.dp, color = fg)
                        Spacer(Modifier.height(6.dp))
                        PixelText(
                            (activity?.label() ?: stringResource(Res.string.ql_add)).uppercase(),
                            style = CobaltTheme.type.small,
                            color = fg,
                            maxLines = 1,
                            align = TextAlign.Center,
                        )
                    }
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun VibeGrid(vibes: List<Vibe>, selectedId: String?, onSelect: (String) -> Unit) {
    vibes.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 4.dp)) {
            row.forEach { vibe ->
                PixelButton(
                    onClick = { onSelect(vibe.id) },
                    filled = vibe.id == selectedId,
                    modifier = Modifier.weight(1f),
                    contentDescription = vibe.label(),
                ) { fg ->
                    PixelText(vibe.kaomoji, style = CobaltTheme.type.user, color = fg, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    PixelText(vibe.label().uppercase(), style = CobaltTheme.type.label, color = fg)
                }
            }
            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal, countedThisHour: Boolean, onToggle: () -> Unit, onMarkAchieved: () -> Unit) {
    PixelCard(padding = 10.dp, shadow = Cobalt.ShadowSmall) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PixelText(goal.name, style = CobaltTheme.type.user, maxLines = 1)
                PixelText(
                    stringResource(Res.string.ql_goal_progress, goal.progressHours, goal.targetHours) +
                        if (goal.achieved) "  " + stringResource(Res.string.ql_goal_achieved) else "",
                    style = CobaltTheme.type.small,
                )
            }
            PixelTextButton(stringResource(Res.string.ql_goal_plus), onToggle, filled = countedThisHour)
        }
        Spacer(Modifier.height(8.dp))
        // Segmented progress; can run past 100% because achieving is the user's call.
        val segments = 10
        val filled = if (goal.targetHours == 0) segments else (goal.progressHours * segments / goal.targetHours).coerceAtMost(segments)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(segments) { i ->
                Box(
                    Modifier.weight(1f).height(8.dp).inkBorder(1.dp)
                        .then(if (i < filled) Modifier.background(Cobalt.Ink) else Modifier),
                )
            }
        }
        if (!goal.achieved && goal.progressHours >= goal.targetHours && goal.targetHours > 0) {
            Spacer(Modifier.height(8.dp))
            PixelText(
                "[ ${stringResource(Res.string.ql_goal_mark_achieved)} ]",
                style = CobaltTheme.type.label,
                modifier = Modifier.clickable(onClick = onMarkAchieved).padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    fieldHint: String,
    withNumber: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("10") }
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(Modifier.fillMaxWidth()) {
            PixelText(title, style = CobaltTheme.type.title)
            Spacer(Modifier.height(12.dp))
            PixelTextField(name, { name = it }, fieldHint, maxLength = 24, singleLine = true)
            if (withNumber) {
                Spacer(Modifier.height(12.dp))
                PixelText(stringResource(Res.string.ql_goal_target), style = CobaltTheme.type.small)
                Spacer(Modifier.height(4.dp))
                PixelTextField(
                    hours,
                    { v -> hours = v.filter { it.isDigit() }.take(4) },
                    "10",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelTextButton(stringResource(Res.string.cancel), onDismiss, Modifier.weight(1f))
                val target = hours.toIntOrNull() ?: 0
                PixelTextButton(
                    stringResource(Res.string.create),
                    { onCreate(name, target) },
                    Modifier.weight(1f),
                    filled = true,
                    enabled = name.isNotBlank() && (!withNumber || target > 0),
                )
            }
        }
    }
}
