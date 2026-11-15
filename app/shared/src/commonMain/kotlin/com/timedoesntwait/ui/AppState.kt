package com.timedoesntwait.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timedoesntwait.domain.HourSlot
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

    /** Opens Quick Log for [slot], e.g. from a notification tap or Timeline "Fill in". */
    fun openQuickLog(slot: HourSlot?) {
        if (Route.Main in stack) {
            while (current != Route.Main) stack.removeAt(stack.lastIndex)
        }
        tab = Tab.LOG
        logSlot = slot
    }
}
