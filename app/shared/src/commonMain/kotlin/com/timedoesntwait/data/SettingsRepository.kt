package com.timedoesntwait.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.timedoesntwait.db.AppDatabase
import com.timedoesntwait.domain.AppLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Phone-only settings. These never sync. */
class SettingsRepository(db: AppDatabase, private val io: CoroutineDispatcher) {
    private val q = db.journalQueries

    fun get(key: String): String? = q.getSetting(key).executeAsOneOrNull()

    fun put(key: String, value: String) {
        q.putSetting(key, value)
    }

    fun observe(key: String): Flow<String?> = q.getSetting(key).asFlow().mapToOneOrNull(io)

    /** Null = follow the phone's language. */
    var language: AppLanguage?
        get() = get(KEY_LANGUAGE)?.let { if (it == "system") null else AppLanguage.fromTag(it) }
        set(value) = put(KEY_LANGUAGE, value?.tag ?: "system")

    var onboardingDone: Boolean
        get() = get(KEY_ONBOARDING_DONE) == "true"
        set(value) = put(KEY_ONBOARDING_DONE, value.toString())

    fun onboardingDoneFlow(): Flow<Boolean> = observe(KEY_ONBOARDING_DONE).map { it == "true" }

    /** Hour of the separate daily-review reminder; null = off. Design doc default: 22:00. */
    var reviewReminderHour: Int?
        get() = when (val v = get(KEY_REVIEW_REMINDER_HOUR)) {
            null -> DEFAULT_REVIEW_REMINDER_HOUR
            "off" -> null
            else -> v.toIntOrNull()?.takeIf { it in 0..23 } ?: DEFAULT_REVIEW_REMINDER_HOUR
        }
        set(value) = put(KEY_REVIEW_REMINDER_HOUR, value?.toString() ?: "off")

    fun reviewReminderHourFlow(): Flow<Int?> = observe(KEY_REVIEW_REMINDER_HOUR).map { reviewReminderHour }

    /** How late the last check-in alarm fired, in minutes. Used for the battery-saver tip. */
    var lastAlarmLatenessMinutes: Int
        get() = get(KEY_LAST_LATENESS)?.toIntOrNull() ?: 0
        set(value) = put(KEY_LAST_LATENESS, value.toString())

    fun lastAlarmLatenessFlow(): Flow<Int> = observe(KEY_LAST_LATENESS).map { it?.toIntOrNull() ?: 0 }

    companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_REVIEW_REMINDER_HOUR = "review_reminder_hour"
        const val KEY_LAST_LATENESS = "last_alarm_lateness_min"
        const val DEFAULT_REVIEW_REMINDER_HOUR = 22
    }
}
