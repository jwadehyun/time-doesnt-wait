package com.timedoesntwait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.ReviewRepository
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.Goal
import com.timedoesntwait.domain.RatingDimension
import com.timedoesntwait.domain.RatingKind
import com.timedoesntwait.domain.Vibe
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.ed_new_activity
import com.timedoesntwait.resources.ed_new_rating
import com.timedoesntwait.resources.ed_new_vibe
import com.timedoesntwait.resources.ed_tap_to_edit
import com.timedoesntwait.resources.ql_goal_achieved
import com.timedoesntwait.resources.ql_goal_new
import com.timedoesntwait.resources.ql_goal_progress
import com.timedoesntwait.resources.re_hearts
import com.timedoesntwait.resources.re_segments
import com.timedoesntwait.resources.re_stars
import com.timedoesntwait.resources.settings_activities
import com.timedoesntwait.resources.settings_goals
import com.timedoesntwait.resources.settings_ratings
import com.timedoesntwait.resources.settings_vibes
import com.timedoesntwait.resources.st_achieved
import com.timedoesntwait.ui.SubTopBar
import com.timedoesntwait.ui.components.ActivityIcon
import com.timedoesntwait.ui.components.InkDivider
import com.timedoesntwait.ui.components.PixelIcon
import com.timedoesntwait.ui.components.PixelText
import com.timedoesntwait.ui.components.PixelTextButton
import com.timedoesntwait.ui.components.SectionCard
import com.timedoesntwait.ui.label
import com.timedoesntwait.ui.theme.Cobalt
import com.timedoesntwait.ui.theme.CobaltTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// Settings → Journal: one list screen per kind of item. Tap a row to edit or delete it.

@Composable
private fun EditorList(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubTopBar(title, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PixelText(stringResource(Res.string.ed_tap_to_edit), style = CobaltTheme.type.small)
            content()
        }
    }
}

@Composable
private fun EditorRow(onClick: () -> Unit, leading: (@Composable () -> Unit)?, title: String, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(36.dp).background(Cobalt.Ink), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            PixelText(title, style = CobaltTheme.type.user, maxLines = 1)
            if (subtitle != null) PixelText(subtitle, style = CobaltTheme.type.small, maxLines = 1)
        }
        PixelIcon("pencil", size = 16.dp)
    }
}

@Composable
private fun <T> ItemsCard(title: String, items: List<T>, row: @Composable (T) -> Unit) {
    if (items.isEmpty()) return
    SectionCard(title) {
        items.forEachIndexed { i, item ->
            if (i > 0) InkDivider()
            row(item)
        }
    }
}

@Composable
fun ActivitiesEditorScreen(onBack: () -> Unit) {
    val repo = koinInject<JournalRepository>()
    val scope = rememberCoroutineScope()
    val activities by remember { repo.activities() }.collectAsState(emptyList())
    var editing by remember { mutableStateOf<Activity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.settings_activities)

    EditorList(title.uppercase(), onBack) {
        ItemsCard("// ${title.uppercase()}", activities) { a ->
            EditorRow({ editing = a }, { ActivityIcon(a.icon, size = 22.dp, color = Cobalt.Card) }, a.label())
        }
        PixelTextButton(stringResource(Res.string.ed_new_activity), { adding = true }, Modifier.fillMaxWidth())
    }
    if (adding) {
        ActivityEditorDialog(null, { adding = false }, { name, icon ->
            scope.launch { repo.addActivity(name, icon); adding = false }
        }, null)
    }
    editing?.let { a ->
        ActivityEditorDialog(
            a,
            { editing = null },
            { name, icon -> scope.launch { repo.updateActivity(a.id, name, icon); editing = null } },
            { scope.launch { repo.deleteActivity(a.id); editing = null } },
        )
    }
}

@Composable
fun VibesEditorScreen(onBack: () -> Unit) {
    val repo = koinInject<JournalRepository>()
    val scope = rememberCoroutineScope()
    val vibes by remember { repo.vibes() }.collectAsState(emptyList())
    var editing by remember { mutableStateOf<Vibe?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.settings_vibes)

    EditorList(title.uppercase(), onBack) {
        ItemsCard("// ${title.uppercase()}", vibes) { v ->
            EditorRow({ editing = v }, null, v.kaomoji, v.label())
        }
        PixelTextButton(stringResource(Res.string.ed_new_vibe), { adding = true }, Modifier.fillMaxWidth())
    }
    if (adding) {
        VibeEditorDialog(null, { adding = false }, { label, kaomoji ->
            scope.launch { repo.addVibe(label, kaomoji); adding = false }
        }, null)
    }
    editing?.let { v ->
        VibeEditorDialog(
            v,
            { editing = null },
            { label, kaomoji -> scope.launch { repo.updateVibe(v.id, label, kaomoji); editing = null } },
            { scope.launch { repo.deleteVibe(v.id); editing = null } },
        )
    }
}

@Composable
fun GoalsEditorScreen(onBack: () -> Unit) {
    val repo = koinInject<JournalRepository>()
    val scope = rememberCoroutineScope()
    val goals by remember { repo.goals() }.collectAsState(emptyList())
    var editing by remember { mutableStateOf<Goal?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.settings_goals)

    @Composable
    fun goalRow(g: Goal) = EditorRow(
        { editing = g },
        { PixelIcon(if (g.achieved) "star" else "check", size = 20.dp, color = Cobalt.Card) },
        g.name,
        stringResource(Res.string.ql_goal_progress, g.progressHours, g.targetHours) +
            if (g.achieved) "  " + stringResource(Res.string.ql_goal_achieved) else "",
    )

    EditorList(title.uppercase(), onBack) {
        ItemsCard("// ${title.uppercase()}", goals.filter { !it.achieved }) { goalRow(it) }
        PixelTextButton(stringResource(Res.string.ql_goal_new), { adding = true }, Modifier.fillMaxWidth())
        ItemsCard("// ${stringResource(Res.string.st_achieved)}", goals.filter { it.achieved }) { goalRow(it) }
    }
    if (adding) {
        GoalEditorDialog(null, { adding = false }, { name, hours, _ ->
            scope.launch { repo.addGoal(name, hours); adding = false }
        }, null)
    }
    editing?.let { g ->
        GoalEditorDialog(
            g,
            { editing = null },
            { name, hours, achieved -> scope.launch { repo.updateGoal(g.id, name, hours, achieved); editing = null } },
            { scope.launch { repo.deleteGoal(g.id); editing = null } },
        )
    }
}

@Composable
fun RatingsEditorScreen(onBack: () -> Unit) {
    val reviews = koinInject<ReviewRepository>()
    val scope = rememberCoroutineScope()
    val dimensions by remember { reviews.ratingDimensions() }.collectAsState(emptyList())
    var editing by remember { mutableStateOf<RatingDimension?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.settings_ratings)

    EditorList(title.uppercase(), onBack) {
        ItemsCard("// ${title.uppercase()}", dimensions.filter { !it.archived }) { d ->
            EditorRow(
                { editing = d },
                {
                    PixelIcon(
                        when (d.kind) {
                            RatingKind.STARS_5 -> "star"
                            RatingKind.HEARTS_5 -> "heart"
                            RatingKind.SEGMENTS_10 -> "bars"
                        },
                        size = 20.dp,
                        color = Cobalt.Card,
                    )
                },
                d.label(),
                stringResource(
                    when (d.kind) {
                        RatingKind.STARS_5 -> Res.string.re_stars
                        RatingKind.HEARTS_5 -> Res.string.re_hearts
                        RatingKind.SEGMENTS_10 -> Res.string.re_segments
                    },
                ),
            )
        }
        PixelTextButton(stringResource(Res.string.ed_new_rating), { adding = true }, Modifier.fillMaxWidth())
    }
    if (adding) {
        RatingEditorDialog(null, { adding = false }, { name, kind ->
            scope.launch { reviews.addRatingDimension(name, kind); adding = false }
        }, null)
    }
    editing?.let { d ->
        RatingEditorDialog(
            d,
            { editing = null },
            { name, kind -> scope.launch { reviews.updateRatingDimension(d.id, name, kind); editing = null } },
            { scope.launch { reviews.deleteRatingDimension(d.id); editing = null } },
        )
    }
}
