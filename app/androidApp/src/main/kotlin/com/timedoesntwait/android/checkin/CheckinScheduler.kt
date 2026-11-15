package com.timedoesntwait.android.checkin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.data.SettingsRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Keeps exactly one alarm pending: the next check-in. When it fires, the receiver posts the
 * notification and calls [scheduleNext] again. Times are local wall-clock, so the schedule
 * follows time-zone and daylight-saving changes (the reschedule receiver rebuilds it).
 */
class CheckinScheduler(
    private val context: Context,
    private val schedule: ScheduleRepository,
    private val settings: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleIfOnboarded() {
        if (settings.onboardingDone) scheduleNext() else cancel()
    }

    fun scheduleNext() {
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        val next = schedule.current().nextPing(now)
        if (next == null) {
            cancel()
            return
        }
        val triggerAt = next.toInstant(zone).toEpochMilliseconds()
        val pending = pendingIntent(triggerAt)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Fallback when "Alarms & reminders" is denied: may arrive up to ~10 min late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        Log.i(TAG, "Next check-in at $next (exact=${canScheduleExact()})")
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(0))
    }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(triggerAt: Long): PendingIntent {
        val intent = Intent(context, CheckinAlarmReceiver::class.java)
            .setAction(CheckinAlarmReceiver.ACTION_PING)
            .putExtra(CheckinAlarmReceiver.EXTRA_PING_AT, triggerAt)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_PING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "CheckinScheduler"
        private const val REQUEST_PING = 1

        fun pingTime(epochMillis: Long): LocalDateTime =
            kotlin.time.Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
