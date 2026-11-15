package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.cancel
import com.timedoesntwait.resources.create
import com.timedoesntwait.resources.ql_activity_name
import com.timedoesntwait.resources.ql_delete_activity
import com.timedoesntwait.resources.ql_delete_confirm
import com.timedoesntwait.resources.ql_delete_note
import com.timedoesntwait.resources.ql_edit_activity
import com.timedoesntwait.resources.ql_emoji_hint
import com.timedoesntwait.resources.ql_icon
import com.timedoesntwait.resources.ql_new_activity
import com.timedoesntwait.resources.save
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelIcons
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.PixelTextField
import com.timedoesntwait.ui.components.hardShadow
import com.timedoesntwait.ui.components.inkBorder
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Create or edit an activity: name + icon (one of the pixel icons, or any emoji typed in).
 * [existing] null = new activity. [onDelete] is only offered when editing.
 */
@Composable
fun ActivityEditorDialog(
    existing: Activity?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val startName = existing?.label().orEmpty()
    val startIsEmoji = existing != null && existing.icon !in PixelIcons.all
    var name by remember { mutableStateOf(startName) }
    var pixelIcon by remember { mutableStateOf(existing?.icon?.takeIf { it in PixelIcons.all } ?: "star") }
    var emoji by remember { mutableStateOf(if (startIsEmoji) existing!!.icon else "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val icon = emoji.trim().ifEmpty { pixelIcon }

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.width(44.dp).height(44.dp).hardShadow(Cobalt.ShadowSmall).background(Cobalt.Ink),
                        contentAlignment = Alignment.Center,
                    ) { ActivityIcon(icon, size = 26.dp, color = Cobalt.Card) }
                    Spacer(Modifier.width(12.dp))
                    PixelText(
                        stringResource(if (existing == null) Res.string.ql_new_activity else Res.string.ql_edit_activity),
                        style = CobaltTheme.type.title,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PixelTextField(name, { name = it }, stringResource(Res.string.ql_activity_name), maxLength = 16, singleLine = true)

                Spacer(Modifier.height(14.dp))
                PixelText(stringResource(Res.string.ql_icon), style = CobaltTheme.type.label)
                Spacer(Modifier.height(8.dp))
                PixelIcons.activityChoices.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { choice ->
                            val selected = emoji.isBlank() && choice == pixelIcon
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.4f)
                                    .background(if (selected) Cobalt.Ink else Cobalt.Card)
                                    .inkBorder()
                                    .clickable(role = Role.RadioButton) {
                                        pixelIcon = choice
                                        emoji = ""
                                    }
                                    .semantics {
                                        contentDescription = choice
                                        this.selected = selected
                                    },
                                contentAlignment = Alignment.Center,
                            ) { PixelIcon(choice, size = 22.dp, color = if (selected) Cobalt.Card else Cobalt.Ink) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                PixelTextField(emoji, { emoji = it.trim() }, stringResource(Res.string.ql_emoji_hint), maxLength = 8, singleLine = true)

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelTextButton(stringResource(Res.string.cancel), onDismiss, Modifier.weight(1f))
                    PixelTextButton(
                        stringResource(if (existing == null) Res.string.create else Res.string.save),
                        {
                            // An untouched built-in name stays the built-in, so it keeps translating (Grind / 업무).
                            val unchanged = existing != null && name.trim() == startName
                            onSave(if (unchanged) existing!!.name else name.trim(), icon)
                        },
                        Modifier.weight(1f),
                        filled = true,
                        enabled = name.isNotBlank(),
                    )
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(16.dp))
                    PixelTextButton(
                        stringResource(if (confirmDelete) Res.string.ql_delete_confirm else Res.string.ql_delete_activity),
                        { if (confirmDelete) onDelete() else confirmDelete = true },
                        Modifier.fillMaxWidth(),
                        danger = true,
                        filled = confirmDelete,
                    )
                    Spacer(Modifier.height(6.dp))
                    PixelText(stringResource(Res.string.ql_delete_note), style = CobaltTheme.type.small)
                }
            }
        }
    }
}
