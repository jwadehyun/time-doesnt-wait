package com.timedoesntwait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timedoesntwait.platform.PermissionStatus
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.platform.SetupPermission
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.perm_alarms
import com.timedoesntwait.resources.perm_alarms_desc
import com.timedoesntwait.resources.perm_battery
import com.timedoesntwait.resources.perm_battery_desc
import com.timedoesntwait.resources.perm_battery_samsung
import com.timedoesntwait.resources.perm_battery_xiaomi
import com.timedoesntwait.resources.perm_fix
import com.timedoesntwait.resources.perm_intro
import com.timedoesntwait.resources.perm_lock
import com.timedoesntwait.resources.perm_lock_desc
import com.timedoesntwait.resources.perm_na
import com.timedoesntwait.resources.perm_next
import com.timedoesntwait.resources.perm_notifications
import com.timedoesntwait.resources.perm_notifications_desc
import com.timedoesntwait.resources.perm_ok
import com.timedoesntwait.resources.perm_skip
import com.timedoesntwait.resources.perm_title
import com.timedoesntwait.resources.welcome_step
import com.timedoesntwait.ui.SubTopBar
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Setup step 2/3, also reachable from Settings → Permissions. */
@Composable
fun PermissionsScreen(setupStep: Int?, onBack: () -> Unit, onNext: () -> Unit) {
    val platform = koinInject<Platform>()
    val state by platform.permissions.collectAsState()
    LaunchedEffect(Unit) { platform.refreshPermissions() }

    Column(Modifier.fillMaxSize()) {
        SubTopBar(
            title = stringResource(Res.string.perm_title),
            onBack = onBack,
            trailing = setupStep?.let { stringResource(Res.string.welcome_step, it, 3) },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PixelText(stringResource(Res.string.perm_intro), style = CobaltTheme.type.body)
            PermissionCard(SetupPermission.NOTIFICATIONS, "bell", Res.string.perm_notifications, Res.string.perm_notifications_desc, state.of(SetupPermission.NOTIFICATIONS), platform)
            PermissionCard(SetupPermission.EXACT_ALARMS, "clock", Res.string.perm_alarms, Res.string.perm_alarms_desc, state.of(SetupPermission.EXACT_ALARMS), platform)
            PermissionCard(
                SetupPermission.BATTERY, "bolt", Res.string.perm_battery, Res.string.perm_battery_desc,
                state.of(SetupPermission.BATTERY), platform,
                extra = when (platform.manufacturer.lowercase()) {
                    "samsung" -> Res.string.perm_battery_samsung
                    "xiaomi", "redmi", "poco" -> Res.string.perm_battery_xiaomi
                    else -> null
                },
            )
            PermissionCard(SetupPermission.LOCK_SCREEN, "chat", Res.string.perm_lock, Res.string.perm_lock_desc, state.of(SetupPermission.LOCK_SCREEN), platform)
            Spacer(Modifier.height(4.dp))
            PixelTextButton(stringResource(Res.string.perm_next), onNext, Modifier.fillMaxWidth(), filled = true, big = true)
            if (setupStep != null && !state.allGood) {
                PixelTextButton(stringResource(Res.string.perm_skip), onNext, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permission: SetupPermission,
    icon: String,
    title: StringResource,
    desc: StringResource,
    status: PermissionStatus,
    platform: Platform,
    extra: StringResource? = null,
) {
    PixelCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(icon, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            PixelText(stringResource(title), style = CobaltTheme.type.label, modifier = Modifier.weight(1f))
            when (status) {
                PermissionStatus.GRANTED -> PixelText(stringResource(Res.string.perm_ok), style = CobaltTheme.type.label)
                PermissionStatus.NOT_APPLICABLE -> PixelText(stringResource(Res.string.perm_na), style = CobaltTheme.type.small)
                PermissionStatus.NEEDED -> PixelTextButton(
                    stringResource(Res.string.perm_fix),
                    onClick = { platform.requestPermission(permission) },
                    danger = true,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        PixelText(stringResource(desc), style = CobaltTheme.type.small)
        if (extra != null && status == PermissionStatus.NEEDED) {
            Spacer(Modifier.height(6.dp))
            PixelText(stringResource(extra), style = CobaltTheme.type.small, color = Cobalt.Danger)
        }
    }
}
