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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.timedoesntwait.resources.perm_banner_alarms
import com.timedoesntwait.resources.perm_banner_late
import com.timedoesntwait.resources.perm_banner_notifications
import com.timedoesntwait.resources.ql_activity
import com.timedoesntwait.resources.ql_add
import com.timedoesntwait.resources.ql_details
import com.timedoesntwait.resources.ql_edit
import com.timedoesntwait.resources.ql_edit_done
import com.timedoesntwait.resources.cancel
import com.timedoesntwait.resources.ql_goal_achieved
import com.timedoesntwait.resources.ql_goal_confirm
import com.timedoesntwait.resources.ql_goal_mark_achieved
import com.timedoesntwait.resources.ql_goal_new
import com.timedoesntwait.resources.ql_goal_plus
import com.timedoesntwait.resources.ql_goal_progress
import com.timedoesntwait.resources.ql_goals
import com.timedoesntwait.resources.ql_header
import com.timedoesntwait.resources.ql_hold_to_edit
import com.timedoesntwait.resources.ql_logged_already
import com.timedoesntwait.resources.ql_question
import com.timedoesntwait.resources.ql_quick_hint
import com.timedoesntwait.resources.ql_save
import com.timedoesntwait.resources.ql_send
import com.timedoesntwait.resources.ql_vibe
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.ui.components.Banner
import com.timedoesntwait.ui.components.IconSquareButton
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
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The Log tab. Top: a message-style composer (type + send) that logs the
 * hour without choosing anything else. Below: optional details (activity, vibe, goals).
 * Saving either way jumps to the Timeline via [onSaved].
 */
@Composable
fun QuickLogScreen(
    requestedSlot: HourSlot?,
    onSlotChange: (HourSlot?) -> Unit,
    onOpenPermissions: () -> Unit,
    onSaved: (HourSlot) -> Unit,
) {
    val repo = koinInject<JournalRepository>()
    val settings = koinInject<SettingsRepository>()
    val platform = koinInject<Platform>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language
    val now by rememberNow()

    val slot = requestedSlot ?: currentSlot(now)
    val activities by remember { repo.activities() }.collectAsState(emptyList())
    val vibes by remember { repo.vibes() }.collectAsState(emptyList())
    val goals by remember { repo.goals() }.collectAsState(emptyList())
    val permissions by platform.permissions.collectAsState()
    val lateness by remember { settings.lastAlarmLatenessFlow() }.collectAsState(0)

    var activityId by remember { mutableStateOf<String?>(null) }
    var vibeId by remember { mutableStateOf<String?>(null) }
    var memo by remember { mutableStateOf("") }
    var goalIds by remember { mutableStateOf(emptySet<String>()) }
    var alreadyLogged by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Activity?>(null) }
    var showNewActivity by remember { mutableStateOf(false) }
    var showNewGoal by remember { mutableStateOf(false) }
    var confirmAchieved by remember { mutableStateOf<Goal?>(null) }

    LaunchedEffect(slot) {
        val existing = repo.entryFor(slot)
        activityId = existing?.activityId
        vibeId = existing?.vibeId
        memo = existing?.memo.orEmpty()
        goalIds = repo.goalIdsFor(slot)
        alreadyLogged = existing != null
    }
    // A deleted activity can't stay selected.
    LaunchedEffect(activities) {
        if (activityId != null && activities.isNotEmpty() && activities.none { it.id == activityId }) activityId = null
    }

    fun save(text: String = memo) {
        if (saving) return
        saving = true
        scope.launch {
            try {
                repo.saveFromApp(
                    slot = slot,
                    activityId = activityId,
                    vibeId = vibeId,
                    memo = text,
                    goalIds = goalIds,
                    source = if (pingForSlot(slot) < now.minusHours(1)) EntrySource.BACKFILL else EntrySource.APP,
                )
                onSaved(slot)
            } finally {
                saving = false
            }
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

        // Quick reply: type and send, like a message. Nothing else needs choosing.
        PixelCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            PixelText(stringResource(Res.string.ql_question, TimeLabels.hourRange(slot.hour, lang)), style = CobaltTheme.type.title)
            if (alreadyLogged) {
                Spacer(Modifier.height(4.dp))
                PixelText(stringResource(Res.string.ql_logged_already), style = CobaltTheme.type.small)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    placeholder = stringResource(Res.string.ql_quick_hint),
                    maxLength = MEMO_MAX_LENGTH,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (memo.isNotBlank()) save() }),
                )
                Spacer(Modifier.width(10.dp))
                IconSquareButton(
                    icon = "send",
                    description = stringResource(Res.string.ql_send),
                    onClick = { if (memo.isNotBlank()) save() },
                    filled = memo.isNotBlank(),
                )
            }
        }

        // Optional details
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(2.dp).background(Cobalt.Ink))
            PixelText(stringResource(Res.string.ql_details), style = CobaltTheme.type.small, modifier = Modifier.padding(horizontal = 10.dp))
            Box(Modifier.weight(1f).height(2.dp).background(Cobalt.Ink))
        }

        // Activities: tap to choose (tap again to clear), hold or [ EDIT ] to rename / change icon / delete.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PixelText(stringResource(Res.string.ql_activity), style = CobaltTheme.type.label, modifier = Modifier.weight(1f))
            PixelText(
                stringResource(if (editMode) Res.string.ql_edit_done else Res.string.ql_edit),
                style = CobaltTheme.type.label,
                modifier = Modifier.clickable { editMode = !editMode }.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        }
        PixelText(stringResource(Res.string.ql_hold_to_edit), style = CobaltTheme.type.small)
        ActivityGrid(
            activities = activities,
            selectedId = activityId,
            editMode = editMode,
            onSelect = { activityId = if (activityId == it) null else it },
            onEdit = { editing = it },
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
                // Tapping an open goal offers to mark it achieved, even before it reaches its target.
                onClick = if (goal.achieved) null else { { confirmAchieved = goal } },
            )
        }
        PixelTextButton(stringResource(Res.string.ql_goal_new), { showNewGoal = true }, Modifier.fillMaxWidth())

        Box(Modifier.fillMaxWidth().halftone(Cobalt.Ink.copy(alpha = 0.35f)).padding(vertical = 12.dp)) {
            PixelTextButton(
                stringResource(Res.string.ql_save),
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                big = true,
                enabled = !saving,
            )
        }
    }

    if (showNewActivity) {
        ActivityEditorDialog(
            existing = null,
            onDismiss = { showNewActivity = false },
            onSave = { name, icon ->
                scope.launch {
                    activityId = repo.addActivity(name, icon).id
                    showNewActivity = false
                }
            },
            onDelete = null,
        )
    }
    editing?.let { activity ->
        ActivityEditorDialog(
            existing = activity,
            onDismiss = { editing = null },
            onSave = { name, icon ->
                scope.launch {
                    repo.updateActivity(activity.id, name, icon)
                    editing = null
                }
            },
            onDelete = {
                scope.launch {
                    repo.deleteActivity(activity.id)
                    if (activityId == activity.id) activityId = null
                    editing = null
                }
            },
        )
    }
    confirmAchieved?.let { goal ->
        MarkAchievedDialog(
            goal = goal,
            onDismiss = { confirmAchieved = null },
            onConfirm = {
                scope.launch {
                    repo.setGoalAchieved(goal.id, true)
                    confirmAchieved = null
                }
            },
        )
    }
    if (showNewGoal) {
        GoalEditorDialog(
            existing = null,
            onDismiss = { showNewGoal = false },
            onSave = { name, hours, _ ->
                scope.launch {
                    repo.addGoal(name, hours)
                    showNewGoal = false
                }
            },
            onDelete = null,
        )
    }
}

private fun previous(slot: HourSlot) =
    if (slot.hour == 0) HourSlot(slot.date.minus(DatePeriod(days = 1)), 23) else HourSlot(slot.date, slot.hour - 1)

private fun next(slot: HourSlot) =
    if (slot.hour == 23) HourSlot(slot.date.plus(DatePeriod(days = 1)), 0) else HourSlot(slot.date, slot.hour + 1)

private fun LocalDateTime.minusHours(h: Int): LocalDateTime {
    val total = hour - h
    return if (total >= 0) LocalDateTime(date, LocalTime(total, minute))
    else LocalDateTime(date.minus(DatePeriod(days = 1)), LocalTime(total + 24, minute))
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
private fun ActivityGrid(
    activities: List<Activity>,
    selectedId: String?,
    editMode: Boolean,
    onSelect: (String) -> Unit,
    onEdit: (Activity) -> Unit,
    onAdd: () -> Unit,
) {
    val tiles: List<Activity?> = activities + null // null = the ADD tile
    tiles.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 4.dp)) {
            row.forEach { activity ->
                val selected = activity != null && activity.id == selectedId && !editMode
                Box(Modifier.weight(1f)) {
                    PixelButton(
                        onClick = {
                            when {
                                activity == null -> onAdd()
                                editMode -> onEdit(activity)
                                else -> onSelect(activity.id)
                            }
                        },
                        onLongClick = activity?.let { { onEdit(it) } },
                        filled = selected,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        contentPadding = PaddingValues(4.dp),
                    ) { fg ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ActivityIcon(activity?.icon ?: "plus", size = 24.dp, color = fg)
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
                    if (editMode && activity != null) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Cobalt.Ink),
                            contentAlignment = Alignment.Center,
                        ) { PixelIcon("pencil", size = 12.dp, color = Cobalt.Card) }
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
private fun GoalRow(
    goal: Goal,
    countedThisHour: Boolean,
    onToggle: () -> Unit,
    onMarkAchieved: () -> Unit,
    onClick: (() -> Unit)?,
) {
    PixelCard(
        modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
        padding = 10.dp,
        shadow = Cobalt.ShadowSmall,
    ) {
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

/** Asked when an open goal is tapped: marks it achieved, whether or not it reached its target hours. */
@Composable
private fun MarkAchievedDialog(goal: Goal, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(Modifier.fillMaxWidth()) {
            PixelText(goal.name, style = CobaltTheme.type.title)
            Spacer(Modifier.height(4.dp))
            PixelText(stringResource(Res.string.ql_goal_progress, goal.progressHours, goal.targetHours), style = CobaltTheme.type.small)
            Spacer(Modifier.height(12.dp))
            PixelText(stringResource(Res.string.ql_goal_confirm), style = CobaltTheme.type.body)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelTextButton(stringResource(Res.string.cancel), onDismiss, Modifier.weight(1f))
                PixelTextButton(stringResource(Res.string.ql_goal_achieved), onConfirm, Modifier.weight(1f), filled = true)
            }
        }
    }
}
