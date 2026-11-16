package com.timedoesntwait.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme

// ── Modifiers ───────────────────────────────────────────────

/** Hard offset shadow with no blur, drawn down-right of the element. */
fun Modifier.hardShadow(offset: Dp = Cobalt.Shadow, color: Color = Cobalt.DeepInk): Modifier =
    if (offset == 0.dp) this else drawBehind {
        val o = offset.toPx()
        drawRect(color, topLeft = Offset(o, o), size = size)
    }

/** 1 dp ink dots every 4 dp. */
fun Modifier.halftone(color: Color = Cobalt.InkFaint, spacing: Dp = Cobalt.DotSpacing): Modifier = drawBehind {
    val step = spacing.toPx()
    val r = Cobalt.Dot.toPx() / 2f
    var y = step / 2
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) step / 2 else step
        while (x < size.width) {
            drawCircle(color, r, Offset(x, y))
            x += step
        }
        y += step
        row++
    }
}

fun Modifier.inkBorder(width: Dp = Cobalt.Border, color: Color = Cobalt.Ink): Modifier =
    border(width, color, RectangleShape)

fun Modifier.dashedBorder(color: Color = Cobalt.Ink, width: Dp = 2.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        style = androidx.compose.ui.graphics.drawscope.Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

// ── Text ────────────────────────────────────────────────────

@Composable
fun PixelText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CobaltTheme.type.body,
    color: Color = Color.Unspecified,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val merged = style.merge(
        TextStyle(
            color = if (color == Color.Unspecified) style.color else color,
            textAlign = align ?: TextAlign.Unspecified,
        ),
    )
    BasicText(text, modifier, merged, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

// ── Buttons ─────────────────────────────────────────────────

/**
 * The 1-bit button: 2 dp border, hard shadow; when pressed it moves 2 dp down-right and the
 * shadow collapses.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
    shadow: Dp = Cobalt.ShadowSmall,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    danger: Boolean = false,
    contentDescription: String? = null,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = if (danger) Cobalt.Danger else Cobalt.Ink
    val bg = when {
        !enabled -> Cobalt.Background
        filled -> ink
        else -> Cobalt.Card
    }
    val fg = if (filled && enabled) Cobalt.Card else if (enabled) ink else Cobalt.InkFaint
    val shift = if (pressed) Cobalt.Press else 0.dp
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = Cobalt.MinTouch)
            .offset(shift, shift)
            .hardShadow(if (pressed || !enabled) 0.dp else shadow, if (danger) Cobalt.Danger else Cobalt.DeepInk)
            .background(bg)
            .inkBorder(color = if (enabled) ink else Cobalt.InkFaint)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { content(fg) }
}

@Composable
fun PixelTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
    big: Boolean = false,
) {
    PixelButton(
        onClick = onClick,
        modifier = modifier,
        filled = filled,
        enabled = enabled,
        danger = danger,
        shadow = if (big) Cobalt.Shadow else Cobalt.ShadowSmall,
        contentPadding = if (big) PaddingValues(16.dp) else PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) { fg ->
        PixelText(
            text,
            style = if (big) CobaltTheme.type.title else CobaltTheme.type.label,
            color = fg,
            align = TextAlign.Center,
        )
    }
}

/** Toggle chip, e.g. EN / KO or a weekday tab. */
@Composable
fun PixelChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    PixelButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = text + if (selected) " ✓" else "" },
        filled = selected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) { fg ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PixelText(text, style = CobaltTheme.type.label, color = fg, align = TextAlign.Center, maxLines = 1)
            if (sub != null) PixelText(sub, style = CobaltTheme.type.small, color = fg, maxLines = 1)
        }
    }
}

// ── Containers ──────────────────────────────────────────────

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    shadow: Dp = Cobalt.Shadow,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .hardShadow(shadow)
            .background(Cobalt.Card)
            .inkBorder()
            .padding(padding),
        content = content,
    )
}

/** Card with a solid ink header strip, like the Settings sections ("// NOTIFICATIONS"). */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.hardShadow().background(Cobalt.Card).inkBorder()) {
        Box(Modifier.fillMaxWidth().background(Cobalt.Ink).padding(horizontal = 12.dp, vertical = 8.dp)) {
            PixelText(title, style = CobaltTheme.type.label, color = Cobalt.Card)
        }
        content()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelText(text, style = CobaltTheme.type.label, modifier = Modifier.weight(1f))
        if (trailing != null) PixelText(trailing, style = CobaltTheme.type.small)
    }
}

@Composable
fun InkDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 0.dp).defaultMinSize(minHeight = 2.dp).background(Cobalt.Ink))
}

/** Warning strip used for permission problems. */
@Composable
fun Banner(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Cobalt.Card)
            .dashedBorder(Cobalt.Danger)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelText("!", style = CobaltTheme.type.title, color = Cobalt.Danger)
        Spacer(Modifier.width(10.dp))
        PixelText(text, style = CobaltTheme.type.small, color = Cobalt.Danger, modifier = Modifier.weight(1f))
        PixelIcon("right", size = 16.dp, color = Cobalt.Danger)
    }
}

// ── Inputs ──────────────────────────────────────────────────

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLength: Int? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** Applied to the editable text itself, e.g. a FocusRequester. */
    fieldModifier: Modifier = Modifier,
) {
    val type = CobaltTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .hardShadow(Cobalt.ShadowSmall)
            .background(Cobalt.Card)
            .inkBorder()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PixelText(">", style = type.label, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) PixelText(placeholder, style = type.user, color = Cobalt.InkFaint)
            BasicTextField(
                value = value,
                onValueChange = { new -> onValueChange(if (maxLength != null) new.take(maxLength) else new) },
                textStyle = type.user,
                cursorBrush = SolidColor(Cobalt.Ink),
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = fieldModifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (maxLength != null) {
            PixelText("${value.length}/$maxLength", style = type.small, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
        }
    }
}

@Composable
fun IconSquareButton(icon: String, description: String, onClick: () -> Unit, filled: Boolean = false) {
    PixelButton(
        onClick = onClick,
        filled = filled,
        contentPadding = PaddingValues(10.dp),
        contentDescription = description,
        modifier = Modifier.size(Cobalt.MinTouch),
    ) { fg -> PixelIcon(icon, size = 20.dp, color = fg) }
}
