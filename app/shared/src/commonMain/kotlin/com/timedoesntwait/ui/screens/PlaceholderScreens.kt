package com.timedoesntwait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.coming_next
import com.timedoesntwait.resources.hours_logged_today
import com.timedoesntwait.resources.review_title
import com.timedoesntwait.resources.stats_title
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.halftone
import com.timedoesntwait.ui.rememberNow
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// Daily Review and Stats are milestone M2. Until then these tabs show today's count.

@Composable
fun ReviewPlaceholderScreen() = Placeholder(Res.string.review_title, "star")

@Composable
fun StatsPlaceholderScreen() = Placeholder(Res.string.stats_title, "bars")

@Composable
private fun Placeholder(title: StringResource, icon: String) {
    val repo = koinInject<JournalRepository>()
    val now by rememberNow()
    val entries by remember(now.date) { repo.entriesFor(now.date) }.collectAsState(emptyList())
    Column(
        Modifier.fillMaxSize().halftone().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PixelCard(Modifier.fillMaxWidth()) {
            PixelIcon(icon, size = 32.dp)
            Spacer(Modifier.height(10.dp))
            PixelText(stringResource(title), style = CobaltTheme.type.title)
            Spacer(Modifier.height(6.dp))
            PixelText(stringResource(Res.string.coming_next), style = CobaltTheme.type.body)
        }
        PixelCard(Modifier.fillMaxWidth()) {
            PixelText(stringResource(Res.string.hours_logged_today), style = CobaltTheme.type.small)
            PixelText(entries.size.toString(), style = CobaltTheme.type.display)
        }
    }
}
