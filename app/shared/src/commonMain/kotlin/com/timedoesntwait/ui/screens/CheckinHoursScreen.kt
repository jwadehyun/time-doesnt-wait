package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.domain.CheckinSchedule
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.ch_clear
import com.timedoesntwait.resources.ch_copy_weekdays
import com.timedoesntwait.resources.ch_copy_weekend
import com.timedoesntwait.resources.ch_day_pings
import com.timedoesntwait.resources.ch_next_ping
import com.timedoesntwait.resources.ch_no_pings
import com.timedoesntwait.resources.ch_overview
import com.timedoesntwait.resources.ch_ping_legend
import com.timedoesntwait.resources.ch_pings
import com.timedoesntwait.resources.ch_preset_awake
import com.timedoesntwait.resources.ch_preset_default
import com.timedoesntwait.resources.ch_preset_work
import com.timedoesntwait.resources.ch_presets
import com.timedoesntwait.resources.ch_quiet_legend
import com.timedoesntwait.resources.ch_save
import com.timedoesntwait.resources.ch_subtitle
import com.timedoesntwait.resources.ch_title
import com.timedoesntwait.resources.welcome_step
import com.timedoesntwait.ui.SubTopBar
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelChip
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.SectionLabel
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.nowLocal
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun CheckinHoursScreen(setupStep: Int?, onBack: () -> Unit, onSaved: () -> Unit) {
    val repo = koinInject<ScheduleRepository>()
    val platform = koinInject<Platform>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language

    var schedule by remember { mutableStateOf<CheckinSchedule?>(null) }
    var day by remember { mutableStateOf(nowLocal().date.dayOfWeek) }
    LaunchedEffect(Unit) { schedule = repo.current() }
    val s = schedule ?: return

    Column(Modifier.fillMaxSize()) {
        SubTopBar(
            title = stringResource(Res.string.ch_title),
            onBack = onBack,
            trailing = setupStep?.let { stringResource(Res.string.welcome_step, it, 3) },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PixelText(stringResource(Res.string.ch_subtitle), style = CobaltTheme.type.body)

            // Weekday tabs
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { d ->
                    PixelChip(
                        text = TimeLabels.weekdayShort(d, lang).uppercase(),
                        selected = d == day,
                        onClick = { day = d },
                        modifier = Modifier.weight(1f),
                        sub = s.pingCount(d).toString(),
                    )
                }
            }

            // 24-hour grid for the selected day
            PixelCard(padding = 12.dp) {
                SectionLabel(
                    TimeLabels.weekdayShort(day, lang).uppercase(),
                    trailing = stringResource(Res.string.ch_day_pings, s.pingCount(day), 24 - s.pingCount(day)),
                )
                Spacer(Modifier.height(8.dp))
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        for (col in 0 until 6) {
                            val hour = row * 6 + col
                            HourCell(
                                hour = hour,
                                on = s.isPing(day, hour),
                                onToggle = { schedule = s.withPing(day, hour, !s.isPing(day, hour)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    PixelText(stringResource(Res.string.ch_ping_legend), style = CobaltTheme.type.small, modifier = Modifier.weight(1f))
                    PixelText(stringResource(Res.string.ch_quiet_legend), style = CobaltTheme.type.small)
                }
            }

            // Presets + copy
            SectionLabel(stringResource(Res.string.ch_presets))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextButton(stringResource(Res.string.ch_preset_default), { schedule = s.withMask(day, CheckinSchedule.window(9, 20)) }, Modifier.weight(1f))
                PixelTextButton(stringResource(Res.string.ch_preset_work), { schedule = s.withMask(day, CheckinSchedule.window(9, 18)) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextButton(stringResource(Res.string.ch_preset_awake), { schedule = s.withMask(day, CheckinSchedule.window(8, 23)) }, Modifier.weight(1f))
                PixelTextButton(stringResource(Res.string.ch_clear), { schedule = s.withMask(day, 0) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextButton(stringResource(Res.string.ch_copy_weekdays), { schedule = s.copyDay(day, CheckinSchedule.WEEKDAYS) }, Modifier.weight(1f))
                PixelTextButton(stringResource(Res.string.ch_copy_weekend), { schedule = s.copyDay(day, CheckinSchedule.WEEKEND) }, Modifier.weight(1f))
            }

            // Weekly overview
            PixelCard(padding = 12.dp) {
                SectionLabel(stringResource(Res.string.ch_overview))
                DayOfWeek.entries.forEach { d ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelText(TimeLabels.weekdayShort(d, lang).uppercase(), style = CobaltTheme.type.small, modifier = Modifier.width(36.dp))
                        Row(Modifier.weight(1f).height(12.dp).inkBorder(1.dp)) {
                            for (h in 0 until 24) {
                                Box(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .then(if (s.isPing(d, h)) Modifier.background(Cobalt.Ink) else Modifier.halftone()),
                                )
                            }
                        }
                        PixelText(
                            stringResource(Res.string.ch_pings, s.pingCount(d)),
                            style = CobaltTheme.type.small,
                            align = TextAlign.End,
                            modifier = Modifier.width(64.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val next = s.nextPing(nowLocal())
                PixelText(
                    if (next == null) stringResource(Res.string.ch_no_pings)
                    else stringResource(
                        Res.string.ch_next_ping,
                        "${TimeLabels.clock(next.hour, lang)} ${TimeLabels.weekdayShort(next.dayOfWeek, lang).uppercase()}",
                    ),
                    style = CobaltTheme.type.label,
                )
            }

            PixelTextButton(
                stringResource(Res.string.ch_save),
                onClick = {
                    scope.launch {
                        repo.save(s)
                        platform.rescheduleCheckins()
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                big = true,
            )
        }
    }
}

@Composable
private fun HourCell(hour: Int, on: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val fg = if (on) Cobalt.Card else Cobalt.Ink
    Column(
        modifier
            .aspectRatio(0.9f)
            .hardShadow(if (on) Cobalt.ShadowSmall else 0.dp)
            .background(if (on) Cobalt.Ink else Cobalt.Card)
            .inkBorder()
            .clickable(role = Role.Switch, onClick = onToggle)
            .semantics {
                contentDescription = "${hour.toString().padStart(2, '0')}:00"
                stateDescription = if (on) "on" else "off"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PixelText(hour.toString().padStart(2, '0'), style = CobaltTheme.type.label, color = fg)
        PixelIcon(if (on) "bolt" else "moon", size = 12.dp, color = fg)
    }
}
