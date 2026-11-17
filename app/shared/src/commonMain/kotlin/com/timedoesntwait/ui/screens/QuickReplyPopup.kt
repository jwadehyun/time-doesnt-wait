package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.domain.Entry
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.MEMO_MAX_LENGTH
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.pingForSlot
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.ql_header
import com.timedoesntwait.resources.ql_question
import com.timedoesntwait.resources.ql_quick_hint
import com.timedoesntwait.resources.ql_send
import com.timedoesntwait.resources.qr_logged
import com.timedoesntwait.resources.qr_more_details
import com.timedoesntwait.ui.components.IconSquareButton
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextField
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The small "message" window that opens when a check-in notification is tapped: type what you did
 * (keyboard already up) and send. No other choices needed.
 * Hosts its own theme so a platform can show it on its own, outside the main app screen.
 */
@Composable
fun QuickReplyPopup(
    slot: HourSlot,
    onLogged: (Entry) -> Unit,
    onMoreDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    val platform = koinInject<Platform>()
    val language by platform.language.collectAsState()
    key(language) {
        CobaltTheme(language) {
            PopupContent(slot, onLogged, onMoreDetails, onDismiss)
        }
    }
}

@Composable
private fun PopupContent(
    slot: HourSlot,
    onLogged: (Entry) -> Unit,
    onMoreDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    val repo = koinInject<JournalRepository>()
    val scope = rememberCoroutineScope()
    val lang = CobaltTheme.language
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        val existing = repo.entryFor(slot)
        if (existing != null && text.isEmpty()) text = existing.memo
        delay(150) // let the window attach before asking for the keyboard
        focus.requestFocus()
        keyboard?.show()
    }

    fun send(value: String) {
        if (value.isBlank() || sending) return
        sending = true
        scope.launch {
            val entry = repo.logText(slot, value.trim(), EntrySource.NOTIFICATION_REPLY)
            keyboard?.hide()
            done = true
            delay(700)
            onLogged(entry)
        }
    }

    // Dimmed backdrop: tapping outside the card closes the window.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .hardShadow()
                .background(Cobalt.Background)
                .inkBorder()
                // Swallow taps so they don't reach the backdrop.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Row(
                Modifier.fillMaxWidth().background(Cobalt.Ink).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelText(
                    stringResource(Res.string.ql_header, TimeLabels.clock(pingForSlot(slot).hour, lang)),
                    style = CobaltTheme.type.label,
                    color = Cobalt.Card,
                    modifier = Modifier.weight(1f),
                )
                PixelText(TimeLabels.date(slot.date, lang).uppercase(), style = CobaltTheme.type.small, color = Cobalt.Card)
            }
            Column(Modifier.padding(14.dp)) {
                PixelText(
                    if (done) "${stringResource(Res.string.qr_logged)} ${TimeLabels.hourRange(slot.hour, lang)}"
                    else stringResource(Res.string.ql_question, TimeLabels.hourRange(slot.hour, lang)),
                    style = CobaltTheme.type.title,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = stringResource(Res.string.ql_quick_hint),
                        maxLength = MEMO_MAX_LENGTH,
                        modifier = Modifier.weight(1f),
                        fieldModifier = Modifier.focusRequester(focus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send(text) }),
                    )
                    Spacer(Modifier.width(10.dp))
                    IconSquareButton(
                        icon = "send",
                        description = stringResource(Res.string.ql_send),
                        onClick = { send(text) },
                        filled = text.isNotBlank(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                PixelText(
                    stringResource(Res.string.qr_more_details),
                    style = CobaltTheme.type.label,
                    align = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onMoreDetails).padding(vertical = 12.dp),
                )
            }
        }
    }
}
