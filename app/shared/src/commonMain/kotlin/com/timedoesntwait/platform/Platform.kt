package com.timedoesntwait.platform

import com.timedoesntwait.domain.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/** The four things Setup checks so check-ins arrive on time and can be answered from the lock screen. */
enum class SetupPermission { NOTIFICATIONS, EXACT_ALARMS, BATTERY, LOCK_SCREEN }

enum class PermissionStatus { GRANTED, NEEDED, NOT_APPLICABLE }

data class PermissionState(val statuses: Map<SetupPermission, PermissionStatus>) {
    fun of(p: SetupPermission): PermissionStatus = statuses[p] ?: PermissionStatus.NEEDED
    val allGood: Boolean get() = statuses.values.none { it == PermissionStatus.NEEDED }
}

/** Implemented by each platform app (Android now, iOS later). */
interface Platform {
    val permissions: StateFlow<PermissionState>

    /** Re-reads permission state, e.g. when the app returns to the foreground. */
    fun refreshPermissions()

    /** Opens the system prompt or settings page that grants [permission]. */
    fun requestPermission(permission: SetupPermission)

    /** Rebuilds the next check-in alarm from the current schedule. */
    fun rescheduleCheckins()

    /** The language the UI is currently shown in. */
    val language: StateFlow<AppLanguage>

    /** Null = follow the phone. */
    fun setLanguage(language: AppLanguage?)

    /** Phone maker, used to show battery-saver tips (e.g. "samsung"). */
    val manufacturer: String

    val appVersion: String
}
