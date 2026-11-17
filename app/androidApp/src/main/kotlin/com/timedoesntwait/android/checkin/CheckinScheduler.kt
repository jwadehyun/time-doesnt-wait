package com.timedoesntwait.android.checkin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.data.SettingsRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
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

    /** Rebuilds both alarms: the next check-in and the evening review reminder. */
    fun scheduleIfOnboarded() {
        if (settings.onboardingDone) {
            scheduleNext()
            scheduleReviewReminder()
        } else {
            cancel()
            alarmManager.cancel(reviewPendingIntent())
        }
    }

    /** The daily-review reminder: its own alarm at the chosen hour (default 22:00), or none when off. */
    fun scheduleReviewReminder() {
        val hour = settings.reviewReminderHour
        val pending = reviewPendingIntent()
        if (hour == null) {
            alarmManager.cancel(pending)
            return
        }
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        val today = LocalDateTime(now.date, LocalTime(hour, 0))
        val next = if (today > now) today else LocalDateTime(now.date.plus(DatePeriod(days = 1)), LocalTime(hour, 0))
        val triggerAt = next.toInstant(zone).toEpochMilliseconds()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        Log.i(TAG, "Next review reminder at $next")
    }

    private fun reviewPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_REVIEW,
        Intent(context, ReviewReminderReceiver::class.java).setAction(ReviewReminderReceiver.ACTION_REVIEW_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
        private const val REQUEST_REVIEW = 2

        fun pingTime(epochMillis: Long): LocalDateTime =
            kotlin.time.Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
