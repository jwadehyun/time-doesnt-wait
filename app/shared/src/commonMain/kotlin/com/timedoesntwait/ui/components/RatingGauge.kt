package com.timedoesntwait.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.ui.theme.Cobalt

/**
 * Stars, hearts or 10 segments. Tapping a score sets it; tapping the current score again clears it
 * (passes null). [onChange] null = read-only. Values above the kind's range are shown capped.
 */
@Composable
fun RatingGauge(
    kind: RatingKind,
    value: Int?,
    onChange: ((Int?) -> Unit)?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    color: Color = Cobalt.Ink,
) {
    val shown = value?.coerceIn(0, kind.max) ?: 0
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (kind == RatingKind.SEGMENTS_10) 4.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..kind.max) {
            val on = i <= shown
            val cell = Modifier
                .weight(1f)
                .then(
                    if (onChange != null) {
                        Modifier
                            .clickable(role = Role.RadioButton) { onChange(if (value == i) null else i) }
                            .semantics {
                                contentDescription = "$i/${kind.max}"
                                selected = value == i
                            }
                    } else {
                        Modifier
                    },
                )
            when (kind) {
                RatingKind.SEGMENTS_10 -> Box(cell.padding(vertical = if (onChange != null) 10.dp else 0.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(iconSize * 0.8f).inkBorder(Cobalt.Border, color)
                            .then(if (on) Modifier.background(color) else Modifier),
                    )
                }
                RatingKind.STARS_5, RatingKind.HEARTS_5 -> Box(
                    cell.height(if (onChange != null) maxOf(iconSize, Cobalt.MinTouch) else iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = if (kind == RatingKind.STARS_5) "star" else "heart"
                    PixelIcon(if (on) icon else "${icon}_outline", size = iconSize, color = color)
                }
            }
        }
    }
}
