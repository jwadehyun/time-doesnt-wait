package com.timedoesntwait.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.StatsPeriod
import kotlinx.datetime.LocalDate

enum class Tab { LOG, TIMELINE, REVIEW, STATS }

sealed interface Route {
    data object Welcome : Route
    data object SetupPermissions : Route
    data object SetupHours : Route
    data object Main : Route
    data object Settings : Route
    data object CheckinHours : Route
    data object Permissions : Route
    data object EditActivities : Route
    data object EditVibes : Route
    data object EditGoals : Route
    data object EditRatings : Route
}

/** Screen stack + selected tab. Back is handled by the platform calling [back]. */
class AppState(onboarded: Boolean) {
    val stack = mutableStateListOf<Route>(if (onboarded) Route.Main else Route.Welcome)
    var tab by mutableStateOf(Tab.LOG)

    /** The hour the Log tab is editing. Null = the hour that just ended. */
    var logSlot by mutableStateOf<HourSlot?>(null)

    /** Day shown on the Timeline. Null = today. */
    var timelineDate by mutableStateOf<LocalDate?>(null)

    /** The hour just saved: the Timeline scrolls to it and marks it. */
    var timelineFocus by mutableStateOf<HourSlot?>(null)

    /** Day shown on the Review tab. Null = today (or an unreviewed yesterday just after midnight). */
    var reviewDate by mutableStateOf<LocalDate?>(null)

    /** Bumped each time the Review tab is opened from outside (e.g. the reminder), so it starts at the top. */
    var reviewOpenCount by mutableStateOf(0)

    var statsPeriod by mutableStateOf(StatsPeriod.WEEK)

    /** A day inside the period Stats shows. Null = today. */
    var statsAnchor by mutableStateOf<LocalDate?>(null)

    val current: Route get() = stack.last()

    fun push(route: Route) {
        stack.add(route)
    }

    fun replaceAll(route: Route) {
        stack.clear()
        stack.add(route)
    }

    /** Returns false when there is nothing to go back to (the platform then closes the app). */
    fun back(): Boolean {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (current == Route.Main && tab != Tab.LOG) {
            tab = Tab.LOG
            return true
        }
        return false
    }

    /** After saving a log: show it on the Timeline. */
    fun showInTimeline(slot: HourSlot) {
        logSlot = null
        timelineDate = slot.date
        timelineFocus = slot
        tab = Tab.TIMELINE
    }

    /** Opens the Review tab on [date], e.g. from the evening reminder. */
    fun openReview(date: LocalDate?) {
        popToMain()
        reviewDate = date
        reviewOpenCount++
        tab = Tab.REVIEW
    }

    /** Opens the Timeline on [date], e.g. from a day on Stats or Review. */
    fun openTimeline(date: LocalDate) {
        timelineDate = date
        timelineFocus = null
        tab = Tab.TIMELINE
    }

    private fun popToMain() {
        if (Route.Main in stack) {
            while (current != Route.Main) stack.removeAt(stack.lastIndex)
        }
    }

    /** Opens Quick Log for [slot], e.g. from a notification tap or Timeline "Fill in". */
    fun openQuickLog(slot: HourSlot?) {
        popToMain()
        tab = Tab.LOG
        logSlot = slot
    }
}
