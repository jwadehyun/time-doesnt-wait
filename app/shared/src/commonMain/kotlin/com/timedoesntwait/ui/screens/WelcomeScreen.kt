package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timedoesntwait.domain.AppLanguage
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.app_name
import com.timedoesntwait.resources.app_tagline
import com.timedoesntwait.resources.lang_en
import com.timedoesntwait.resources.lang_ko
import com.timedoesntwait.resources.welcome_body
import com.timedoesntwait.resources.welcome_language
import com.timedoesntwait.resources.welcome_signin_later
import com.timedoesntwait.resources.welcome_start
import com.timedoesntwait.resources.welcome_step
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelChip
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Setup step 1/3. Sign-in (Google + Kakao) is added here in M3. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val platform = koinInject<Platform>()
    val language by platform.language.collectAsState()
    Column(
        Modifier.fillMaxSize().halftone().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PixelText(stringResource(Res.string.welcome_step, 1, 3), style = CobaltTheme.type.small)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(72.dp).hardShadow().background(Cobalt.Ink).inkBorder(),
            contentAlignment = Alignment.Center,
        ) { PixelIcon("clock", size = 44.dp, color = Cobalt.Card) }
        PixelCard {
            PixelText(stringResource(Res.string.app_name).uppercase(), style = CobaltTheme.type.display)
            Spacer(Modifier.height(6.dp))
            PixelText(stringResource(Res.string.app_tagline), style = CobaltTheme.type.label)
            Spacer(Modifier.height(14.dp))
            PixelText(stringResource(Res.string.welcome_body), style = CobaltTheme.type.body)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelText(stringResource(Res.string.welcome_language), style = CobaltTheme.type.label)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelChip(stringResource(Res.string.lang_en), language == AppLanguage.EN, { platform.setLanguage(AppLanguage.EN) }, Modifier.weight(1f))
                PixelChip(stringResource(Res.string.lang_ko), language == AppLanguage.KO, { platform.setLanguage(AppLanguage.KO) }, Modifier.weight(1f))
            }
        }
        PixelText(stringResource(Res.string.welcome_signin_later), style = CobaltTheme.type.small)
        PixelTextButton(stringResource(Res.string.welcome_start), onStart, Modifier.fillMaxWidth(), filled = true, big = true)
    }
}
