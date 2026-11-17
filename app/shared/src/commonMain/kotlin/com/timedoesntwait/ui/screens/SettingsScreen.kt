package com.timedoesntwait.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.timedoesntwait.data.ReviewRepository
import com.timedoesntwait.resources.cancel
import com.timedoesntwait.resources.rr_note
import com.timedoesntwait.resources.rr_off
import com.timedoesntwait.resources.rr_title
import com.timedoesntwait.resources.settings_active_count
import com.timedoesntwait.resources.settings_activities
import com.timedoesntwait.resources.settings_goals
import com.timedoesntwait.resources.settings_goals_count
import com.timedoesntwait.resources.settings_journal
import com.timedoesntwait.resources.settings_off
import com.timedoesntwait.resources.settings_ratings
import com.timedoesntwait.resources.settings_vibes
import com.timedoesntwait.ui.components.PixelCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.domain.AppLanguage
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.platform.PermissionStatus
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.platform.SetupPermission
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.lang_en
import com.timedoesntwait.resources.lang_ko
import com.timedoesntwait.resources.perm_alarms
import com.timedoesntwait.resources.perm_battery
import com.timedoesntwait.resources.perm_lock
import com.timedoesntwait.resources.perm_notifications
import com.timedoesntwait.resources.settings_checkin_hours
import com.timedoesntwait.resources.settings_checkin_hours_desc
import com.timedoesntwait.resources.settings_data
import com.timedoesntwait.resources.settings_language
import com.timedoesntwait.resources.settings_notifications
import com.timedoesntwait.resources.settings_permissions
import com.timedoesntwait.resources.settings_quick_reply
import com.timedoesntwait.resources.settings_quick_reply_desc
import com.timedoesntwait.resources.settings_review_permissions
import com.timedoesntwait.resources.settings_review_reminder
import com.timedoesntwait.resources.settings_sync_later
import com.timedoesntwait.resources.settings_title
import com.timedoesntwait.resources.settings_unsynced
import com.timedoesntwait.resources.settings_version
import com.timedoesntwait.ui.SubTopBar
import com.timedoesntwait.ui.components.InkDivider
import com.timedoesntwait.ui.components.PixelChip
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.SectionCard
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCheckinHours: () -> Unit,
    onPermissions: () -> Unit,
    onEditActivities: () -> Unit,
    onEditVibes: () -> Unit,
    onEditGoals: () -> Unit,
    onEditRatings: () -> Unit,
) {
    val platform = koinInject<Platform>()
    val settings = koinInject<SettingsRepository>()
    val journal = koinInject<JournalRepository>()
    val reviews = koinInject<ReviewRepository>()
    val activities by remember { journal.activities() }.collectAsState(emptyList())
    val vibes by remember { journal.vibes() }.collectAsState(emptyList())
    val goals by remember { journal.goals() }.collectAsState(emptyList())
    val ratings by remember { reviews.ratingDimensions() }.collectAsState(emptyList())
    val reminderHour by remember { settings.reviewReminderHourFlow() }.collectAsState(settings.reviewReminderHour)
    var pickingReminder by remember { mutableStateOf(false) }
    val language by platform.language.collectAsState()
    val permissions by platform.permissions.collectAsState()
    val unsynced by remember { journal.outboxCount() }.collectAsState(0L)
    val lang = CobaltTheme.language

    Column(Modifier.fillMaxSize()) {
        SubTopBar(stringResource(Res.string.settings_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionCard(stringResource(Res.string.settings_notifications)) {
                SettingRow(Res.string.settings_checkin_hours, stringResource(Res.string.settings_checkin_hours_desc), onCheckinHours)
                InkDivider()
                SettingRow(Res.string.settings_quick_reply, stringResource(Res.string.settings_quick_reply_desc), null)
                InkDivider()
                SettingRow(
                    Res.string.settings_review_reminder,
                    reminderHour?.let { TimeLabels.clock(it, lang) } ?: stringResource(Res.string.settings_off),
                ) { pickingReminder = true }
            }

            SectionCard(stringResource(Res.string.settings_journal)) {
                SettingRow(Res.string.settings_activities, stringResource(Res.string.settings_active_count, activities.size), onEditActivities)
                InkDivider()
                SettingRow(Res.string.settings_vibes, stringResource(Res.string.settings_active_count, vibes.size), onEditVibes)
                InkDivider()
                SettingRow(
                    Res.string.settings_goals,
                    stringResource(Res.string.settings_goals_count, goals.count { !it.achieved }, goals.count { it.achieved }),
                    onEditGoals,
                )
                InkDivider()
                SettingRow(Res.string.settings_ratings, stringResource(Res.string.settings_active_count, ratings.count { !it.archived }), onEditRatings)
            }

            SectionCard(stringResource(Res.string.settings_language)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PixelChip(stringResource(Res.string.lang_en), language == AppLanguage.EN, { platform.setLanguage(AppLanguage.EN) }, Modifier.weight(1f))
                    PixelChip(stringResource(Res.string.lang_ko), language == AppLanguage.KO, { platform.setLanguage(AppLanguage.KO) }, Modifier.weight(1f))
                }
            }

            SectionCard(stringResource(Res.string.settings_permissions)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SetupPermission.NOTIFICATIONS to Res.string.perm_notifications,
                        SetupPermission.EXACT_ALARMS to Res.string.perm_alarms,
                        SetupPermission.BATTERY to Res.string.perm_battery,
                        SetupPermission.LOCK_SCREEN to Res.string.perm_lock,
                    ).forEach { (p, label) ->
                        val status = permissions.of(p)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelText(stringResource(label), style = CobaltTheme.type.body, modifier = Modifier.weight(1f))
                            PixelText(
                                when (status) {
                                    PermissionStatus.GRANTED -> "✓"
                                    PermissionStatus.NOT_APPLICABLE -> "–"
                                    PermissionStatus.NEEDED -> "!"
                                },
                                style = CobaltTheme.type.title,
                                color = if (status == PermissionStatus.NEEDED) Cobalt.Danger else Cobalt.Ink,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    PixelTextButton(stringResource(Res.string.settings_review_permissions), onPermissions, Modifier.fillMaxWidth())
                }
            }

            SectionCard(stringResource(Res.string.settings_data)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelText(stringResource(Res.string.settings_unsynced, unsynced.toInt()), style = CobaltTheme.type.label)
                    PixelText(stringResource(Res.string.settings_sync_later), style = CobaltTheme.type.small)
                }
            }

            PixelText(
                stringResource(Res.string.settings_version, platform.appVersion),
                style = CobaltTheme.type.small,
                align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickingReminder) {
        ReminderDialog(reminderHour, { pickingReminder = false }) { hour ->
            settings.reviewReminderHour = hour
            platform.rescheduleCheckins() // also reschedules the review reminder
            pickingReminder = false
        }
    }
}

/** Picks the evening reminder hour (6 PM – 11 PM) or turns it off. */
@Composable
private fun ReminderDialog(current: Int?, onDismiss: () -> Unit, onPick: (Int?) -> Unit) {
    val lang = CobaltTheme.language
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(Modifier.fillMaxWidth()) {
            PixelText(stringResource(Res.string.rr_title), style = CobaltTheme.type.title)
            Spacer(Modifier.height(6.dp))
            PixelText(stringResource(Res.string.rr_note), style = CobaltTheme.type.small)
            Spacer(Modifier.height(12.dp))
            val options: List<Int?> = (18..23).toList() + null
            options.chunked(2).forEach { row ->
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { hour ->
                        PixelChip(
                            hour?.let { TimeLabels.clock(it, lang) } ?: stringResource(Res.string.rr_off),
                            current == hour,
                            { onPick(hour) },
                            Modifier.weight(1f),
                        )
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            PixelTextButton(stringResource(Res.string.cancel), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingRow(title: StringResource, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            PixelText(stringResource(title), style = CobaltTheme.type.label)
            PixelText(subtitle, style = CobaltTheme.type.small)
        }
        if (onClick != null) PixelIcon("right", size = 16.dp)
    }
}
