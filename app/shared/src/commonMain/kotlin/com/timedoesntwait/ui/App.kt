package com.timedoesntwait.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.app_name
import com.timedoesntwait.resources.back
import com.timedoesntwait.resources.profile
import com.timedoesntwait.resources.tab_log
import com.timedoesntwait.resources.tab_review
import com.timedoesntwait.resources.tab_stats
import com.timedoesntwait.resources.tab_timeline
import com.timedoesntwait.ui.components.IconSquareButton
import com.timedoesntwait.ui.components.InkDivider
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.screens.CheckinHoursScreen
import com.timedoesntwait.ui.screens.PermissionsScreen
import com.timedoesntwait.ui.screens.QuickLogScreen
import com.timedoesntwait.ui.screens.ReviewPlaceholderScreen
import com.timedoesntwait.ui.screens.SettingsScreen
import com.timedoesntwait.ui.screens.StatsPlaceholderScreen
import com.timedoesntwait.ui.screens.TimelineScreen
import com.timedoesntwait.ui.screens.WelcomeScreen
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun App(state: AppState) {
    val platform = koinInject<Platform>()
    val settings = koinInject<SettingsRepository>()
    val language by platform.language.collectAsState()

    // Compose resources read the process locale; re-keying rebuilds the UI when the language changes.
    key(language) {
    CobaltTheme(language) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Cobalt.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when (state.current) {
                Route.Welcome -> WelcomeScreen(onStart = { state.push(Route.SetupPermissions) })
                Route.SetupPermissions -> PermissionsScreen(
                    setupStep = 2,
                    onBack = { state.back() },
                    onNext = { state.push(Route.SetupHours) },
                )
                Route.SetupHours -> CheckinHoursScreen(
                    setupStep = 3,
                    onBack = { state.back() },
                    onSaved = {
                        settings.onboardingDone = true
                        platform.rescheduleCheckins() // the save's own reschedule ran before onboarding was done
                        state.replaceAll(Route.Main)
                    },
                )
                Route.Main -> MainShell(state)
                Route.Settings -> SettingsScreen(
                    onBack = { state.back() },
                    onCheckinHours = { state.push(Route.CheckinHours) },
                    onPermissions = { state.push(Route.Permissions) },
                )
                Route.CheckinHours -> CheckinHoursScreen(
                    setupStep = null,
                    onBack = { state.back() },
                    onSaved = { state.back() },
                )
                Route.Permissions -> PermissionsScreen(
                    setupStep = null,
                    onBack = { state.back() },
                    onNext = { state.back() },
                )
            }
        }
    }
    }
}

@Composable
private fun MainShell(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        MainTopBar(
            title = when (state.tab) {
                Tab.LOG -> stringResource(Res.string.tab_log)
                Tab.TIMELINE -> stringResource(Res.string.tab_timeline)
                Tab.REVIEW -> stringResource(Res.string.tab_review)
                Tab.STATS -> stringResource(Res.string.tab_stats)
            },
            onProfile = { state.push(Route.Settings) },
        )
        Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
            when (state.tab) {
                Tab.LOG -> QuickLogScreen(
                    requestedSlot = state.logSlot,
                    onSlotChange = { state.logSlot = it },
                    onOpenPermissions = { state.push(Route.Permissions) },
                )
                Tab.TIMELINE -> TimelineScreen(onOpenSlot = { state.openQuickLog(it) })
                Tab.REVIEW -> ReviewPlaceholderScreen()
                Tab.STATS -> StatsPlaceholderScreen()
            }
        }
        BottomBar(selected = state.tab, onSelect = {
            if (it == Tab.LOG && state.tab != Tab.LOG) state.logSlot = null
            state.tab = it
        })
    }
}

@Composable
private fun MainTopBar(title: String, onProfile: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                PixelText(stringResource(Res.string.app_name).uppercase(), style = CobaltTheme.type.small)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(12.dp).height(12.dp).background(Cobalt.Ink))
                    Spacer(Modifier.width(8.dp))
                    PixelText(title, style = CobaltTheme.type.display)
                }
            }
            IconSquareButton("user", stringResource(Res.string.profile), onProfile, filled = true)
        }
        InkDivider()
    }
}

/** Top bar for pushed screens: [←] + title. */
@Composable
fun SubTopBar(title: String, onBack: () -> Unit, trailing: String? = null) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquareButton("back", stringResource(Res.string.back), onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                PixelText(stringResource(Res.string.app_name).uppercase(), style = CobaltTheme.type.small)
                PixelText(title, style = CobaltTheme.type.title)
            }
            if (trailing != null) PixelText(trailing, style = CobaltTheme.type.small)
        }
        InkDivider()
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column {
        InkDivider()
        Row(Modifier.fillMaxWidth().background(Cobalt.Background), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                val fg = if (isSelected) Cobalt.Card else Cobalt.Ink
                Column(
                    Modifier
                        .weight(1f)
                        .background(if (isSelected) Cobalt.Ink else Cobalt.Background)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = isSelected }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PixelIcon(
                        when (tab) {
                            Tab.LOG -> "pencil"
                            Tab.TIMELINE -> "clock"
                            Tab.REVIEW -> "star"
                            Tab.STATS -> "bars"
                        },
                        size = 20.dp,
                        color = fg,
                    )
                    Spacer(Modifier.height(4.dp))
                    PixelText(
                        stringResource(
                            when (tab) {
                                Tab.LOG -> Res.string.tab_log
                                Tab.TIMELINE -> Res.string.tab_timeline
                                Tab.REVIEW -> Res.string.tab_review
                                Tab.STATS -> Res.string.tab_stats
                            },
                        ),
                        style = CobaltTheme.type.small,
                        color = fg,
                        align = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
