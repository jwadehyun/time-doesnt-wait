package com.timedoesntwait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.timedoesntwait.domain.Goal
import com.timedoesntwait.domain.RatingDimension
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.domain.Vibe
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.cancel
import com.timedoesntwait.resources.create
import com.timedoesntwait.resources.ge_delete
import com.timedoesntwait.resources.ge_delete_note
import com.timedoesntwait.resources.ge_edit
import com.timedoesntwait.resources.ql_delete_confirm
import com.timedoesntwait.resources.ql_delete_note
import com.timedoesntwait.resources.ql_goal_achieved
import com.timedoesntwait.resources.ql_goal_mark_achieved
import com.timedoesntwait.resources.ql_goal_name
import com.timedoesntwait.resources.ql_goal_progress
import com.timedoesntwait.resources.ql_goal_target
import com.timedoesntwait.resources.ql_new_goal
import com.timedoesntwait.resources.re_delete
import com.timedoesntwait.resources.re_delete_note
import com.timedoesntwait.resources.re_edit
import com.timedoesntwait.resources.re_hearts
import com.timedoesntwait.resources.re_new
import com.timedoesntwait.resources.re_segments
import com.timedoesntwait.resources.re_stars
import com.timedoesntwait.resources.re_style
import com.timedoesntwait.resources.save
import com.timedoesntwait.resources.ve_delete
import com.timedoesntwait.resources.ve_edit
import com.timedoesntwait.resources.ve_kaomoji
import com.timedoesntwait.resources.ve_label
import com.timedoesntwait.resources.ve_new
import com.timedoesntwait.ui.components.PixelCard
import com.timedoesntwait.ui.components.PixelChip
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.PixelTextField
import com.timedoesntwait.ui.components.RatingGauge
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.theme.CobaltTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Shared frame for the small editors: title, fields, CANCEL / SAVE, optional two-tap delete. */
@Composable
private fun EditorDialog(
    title: String,
    isNew: Boolean,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    deleteLabel: StringResource?,
    deleteNote: StringResource?,
    onDelete: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PixelText(title, style = CobaltTheme.type.title)
                Spacer(Modifier.height(14.dp))
                content()
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelTextButton(stringResource(Res.string.cancel), onDismiss, Modifier.weight(1f))
                    PixelTextButton(
                        stringResource(if (isNew) Res.string.create else Res.string.save),
                        onSave,
                        Modifier.weight(1f),
                        filled = true,
                        enabled = canSave,
                    )
                }
                if (onDelete != null && deleteLabel != null) {
                    Spacer(Modifier.height(16.dp))
                    PixelTextButton(
                        stringResource(if (confirmDelete) Res.string.ql_delete_confirm else deleteLabel),
                        { if (confirmDelete) onDelete() else confirmDelete = true },
                        Modifier.fillMaxWidth(),
                        danger = true,
                        filled = confirmDelete,
                    )
                    if (deleteNote != null) {
                        Spacer(Modifier.height(6.dp))
                        PixelText(stringResource(deleteNote), style = CobaltTheme.type.small)
                    }
                }
            }
        }
    }
}

/** Create or edit a review rating: name + style (stars, hearts, 10 bars). */
@Composable
fun RatingEditorDialog(
    existing: RatingDimension?,
    onDismiss: () -> Unit,
    onSave: (name: String, kind: RatingKind) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val startName = existing?.label().orEmpty()
    var name by remember { mutableStateOf(startName) }
    var kind by remember { mutableStateOf(existing?.kind ?: RatingKind.STARS_5) }
    EditorDialog(
        title = stringResource(if (existing == null) Res.string.re_new else Res.string.re_edit),
        isNew = existing == null,
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            // An untouched built-in name stays the built-in, so it keeps translating.
            val unchanged = existing != null && name.trim() == startName
            onSave(if (unchanged) existing!!.name else name.trim(), kind)
        },
        deleteLabel = Res.string.re_delete,
        deleteNote = Res.string.re_delete_note,
        onDelete = onDelete,
    ) {
        PixelTextField(name, { name = it }, stringResource(Res.string.ve_label), maxLength = 20, singleLine = true)
        Spacer(Modifier.height(14.dp))
        PixelText(stringResource(Res.string.re_style), style = CobaltTheme.type.label)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                RatingKind.STARS_5 to Res.string.re_stars,
                RatingKind.HEARTS_5 to Res.string.re_hearts,
                RatingKind.SEGMENTS_10 to Res.string.re_segments,
            ).forEach { (k, label) ->
                PixelChip(stringResource(label), kind == k, { kind = k }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        RatingGauge(kind, value = if (kind == RatingKind.SEGMENTS_10) 6 else 3, onChange = null, iconSize = 24.dp)
    }
}

/** Create or edit a vibe: name + kaomoji (or any emoji). */
@Composable
fun VibeEditorDialog(
    existing: Vibe?,
    onDismiss: () -> Unit,
    onSave: (label: String, kaomoji: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val startLabel = existing?.label().orEmpty()
    var label by remember { mutableStateOf(startLabel) }
    var kaomoji by remember { mutableStateOf(existing?.kaomoji.orEmpty()) }
    EditorDialog(
        title = stringResource(if (existing == null) Res.string.ve_new else Res.string.ve_edit),
        isNew = existing == null,
        canSave = label.isNotBlank() && kaomoji.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            val unchanged = existing != null && label.trim() == startLabel
            onSave(if (unchanged) existing!!.label else label.trim(), kaomoji.trim())
        },
        deleteLabel = Res.string.ve_delete,
        deleteNote = Res.string.ql_delete_note,
        onDelete = onDelete,
    ) {
        PixelTextField(label, { label = it }, stringResource(Res.string.ve_label), maxLength = 12, singleLine = true)
        Spacer(Modifier.height(12.dp))
        PixelTextField(kaomoji, { kaomoji = it }, stringResource(Res.string.ve_kaomoji), maxLength = 12, singleLine = true)
    }
}

/** Create or edit a progress goal: name, target hours and (when editing) achieved. */
@Composable
fun GoalEditorDialog(
    existing: Goal?,
    onDismiss: () -> Unit,
    onSave: (name: String, targetHours: Int, achieved: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var hours by remember { mutableStateOf((existing?.targetHours ?: 10).toString()) }
    var achieved by remember { mutableStateOf(existing?.achieved ?: false) }
    val target = hours.toIntOrNull() ?: 0
    EditorDialog(
        title = stringResource(if (existing == null) Res.string.ql_new_goal else Res.string.ge_edit),
        isNew = existing == null,
        canSave = name.isNotBlank() && target > 0,
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), target, achieved) },
        deleteLabel = Res.string.ge_delete,
        deleteNote = Res.string.ge_delete_note,
        onDelete = onDelete,
    ) {
        PixelTextField(name, { name = it }, stringResource(Res.string.ql_goal_name), maxLength = 24, singleLine = true)
        Spacer(Modifier.height(12.dp))
        PixelText(stringResource(Res.string.ql_goal_target), style = CobaltTheme.type.small)
        Spacer(Modifier.height(4.dp))
        PixelTextField(
            hours,
            { v -> hours = v.filter { it.isDigit() }.take(4) },
            "10",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (existing != null) {
            Spacer(Modifier.height(8.dp))
            PixelText(
                stringResource(Res.string.ql_goal_progress, existing.progressHours, target),
                style = CobaltTheme.type.small,
            )
            Spacer(Modifier.height(12.dp))
            PixelChip(
                stringResource(if (achieved) Res.string.ql_goal_achieved else Res.string.ql_goal_mark_achieved),
                achieved,
                { achieved = !achieved },
                Modifier.fillMaxWidth(),
            )
        }
    }
}
