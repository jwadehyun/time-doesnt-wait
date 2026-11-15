package com.timedoesntwait.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.Vibe
import com.timedoesntwait.domain.usesBuiltInName
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.act_chat
import com.timedoesntwait.resources.act_game
import com.timedoesntwait.resources.act_grind
import com.timedoesntwait.resources.act_nap
import com.timedoesntwait.resources.act_snack
import com.timedoesntwait.resources.act_study
import com.timedoesntwait.resources.act_vibe
import com.timedoesntwait.resources.act_walk
import com.timedoesntwait.resources.vibe_chill
import com.timedoesntwait.resources.vibe_solid
import com.timedoesntwait.resources.vibe_super
import com.timedoesntwait.resources.vibe_tired
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private val activityKeys: Map<String, StringResource> = mapOf(
    "grind" to Res.string.act_grind,
    "snack" to Res.string.act_snack,
    "vibe" to Res.string.act_vibe,
    "study" to Res.string.act_study,
    "walk" to Res.string.act_walk,
    "nap" to Res.string.act_nap,
    "game" to Res.string.act_game,
    "chat" to Res.string.act_chat,
)

private val vibeKeys: Map<String, StringResource> = mapOf(
    "super" to Res.string.vibe_super,
    "solid" to Res.string.vibe_solid,
    "chill" to Res.string.vibe_chill,
    "tired" to Res.string.vibe_tired,
)

/** Built-in names are translated; what the user typed is shown as-is. */
@Composable
fun Activity.label(): String =
    if (usesBuiltInName) nameKey?.let(activityKeys::get)?.let { stringResource(it) } ?: name else name

@Composable
fun Vibe.label(): String = labelKey?.let(vibeKeys::get)?.let { stringResource(it) } ?: label

fun nowLocal(clock: Clock = Clock.System): LocalDateTime =
    clock.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** Local time that refreshes every 30 s, so "missed" rows and the current hour stay correct. */
@Composable
fun rememberNow(): State<LocalDateTime> {
    val state = remember { mutableStateOf(nowLocal()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            state.value = nowLocal()
        }
    }
    return state
}
