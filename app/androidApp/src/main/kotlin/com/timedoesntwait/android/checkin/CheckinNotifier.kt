package com.timedoesntwait.android.checkin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.timedoesntwait.android.AndroidPlatform
import com.timedoesntwait.android.MainActivity
import com.timedoesntwait.android.R
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.domain.Activity
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.domain.TimeLabels
import com.timedoesntwait.domain.pingForSlot
import com.timedoesntwait.domain.usesBuiltInName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Builds the check-in notification. Each hour gets its own notification id, so unanswered
 * check-ins stack up until the user logs them.
 */
class CheckinNotifier(
    private val context: Context,
    private val journal: JournalRepository,
    private val platform: AndroidPlatform,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val c = platform.localizedContext()
        val checkins = NotificationChannel(
            CHANNEL_CHECKINS,
            c.getString(R.string.channel_checkins),
            NotificationManager.IMPORTANCE_HIGH, // heads-up banner; Do Not Disturb is still respected
        ).apply {
            description = c.getString(R.string.channel_checkins_desc)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        val status = NotificationChannel(
            CHANNEL_STATUS,
            c.getString(R.string.channel_status),
            NotificationManager.IMPORTANCE_LOW,
        )
        val review = NotificationChannel(
            CHANNEL_REVIEW,
            c.getString(R.string.channel_review),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = c.getString(R.string.channel_review_desc) }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(checkins, status, review))
    }

    /** Posts the "What did you do 2–3 PM?" notification for [slot]. */
    fun showCheckin(slot: HourSlot) {
        if (!manager.areNotificationsEnabled()) return
        val c = platform.localizedContext()
        val lang = platform.language.value
        val range = TimeLabels.hourRange(slot.hour, lang)

        val remoteInput = RemoteInput.Builder(CheckinReplyReceiver.KEY_REPLY_TEXT)
            .setLabel(c.getString(R.string.checkin_reply_hint))
            .build()
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_checkin,
            c.getString(R.string.checkin_reply),
            replyIntent(slot, activityId = null, mutable = true),
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAuthenticationRequired(false) // reply from the lock screen without unlocking (Android 12+)
            .build()

        val quick = journal.topActivities(slot.date, limit = 2).map { activity ->
            NotificationCompat.Action.Builder(
                R.drawable.ic_stat_checkin,
                activityLabel(c, activity),
                replyIntent(slot, activityId = activity.id, mutable = false),
            ).setAuthenticationRequired(false).build()
        }

        val pingMillis = pingForSlot(slot).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKINS)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setColor(INK)
            .setContentTitle(c.getString(R.string.checkin_title, range))
            .setContentText(c.getString(R.string.checkin_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(c.getString(R.string.checkin_text)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(pingMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(slot))
            .addAction(replyAction)
            .apply { quick.forEach { addAction(it) } } // 3 actions max: Reply + top 2 activities
            .build()

        try {
            manager.notify(notificationId(slot), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the post.
        }
    }

    /**
     * Replaces the check-in with "Logged ✓ 2–3 PM · Snack" and dismisses it a few seconds later.
     * Updating the notification is also what stops Android's reply spinner.
     */
    fun showLogged(slot: HourSlot, activity: Activity?, activityMissing: Boolean = false) {
        val c = platform.localizedContext()
        val lang = platform.language.value
        val label = when {
            activity != null -> activityLabel(c, activity)
            activityMissing -> c.getString(R.string.unknown)
            else -> c.getString(R.string.uncategorized)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKINS)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setColor(INK)
            .setContentTitle(c.getString(R.string.checkin_logged, TimeLabels.hourRange(slot.hour, lang), label))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setTimeoutAfter(LOGGED_VISIBLE_MS)
            .build()
        try {
            manager.notify(notificationId(slot), notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(slot: HourSlot) = manager.cancel(notificationId(slot))

    /** "How was today?" Tapping it opens the Review tab on [date]. */
    fun showReviewReminder(date: LocalDate) {
        if (!manager.areNotificationsEnabled()) return
        val c = platform.localizedContext()
        val open = PendingIntent.getActivity(
            context,
            REVIEW_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_REVIEW)
                .putExtra(EXTRA_DATE, date.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REVIEW)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setColor(INK)
            .setContentTitle(c.getString(R.string.review_title))
            .setContentText(c.getString(R.string.review_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            manager.notify(REVIEW_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelReviewReminder() = manager.cancel(REVIEW_NOTIFICATION_ID)

    private fun replyIntent(slot: HourSlot, activityId: String?, mutable: Boolean): PendingIntent {
        val intent = Intent(context, CheckinReplyReceiver::class.java)
            .setAction(if (activityId == null) CheckinReplyReceiver.ACTION_REPLY else CheckinReplyReceiver.ACTION_QUICK)
            .putExtra(EXTRA_DATE, slot.date.toString())
            .putExtra(EXTRA_HOUR, slot.hour)
            .putExtra(CheckinReplyReceiver.EXTRA_ACTIVITY_ID, activityId)
        // Request codes must differ per hour (and per button) or PendingIntents overwrite each other.
        val code = notificationId(slot) * 4 + (activityId?.hashCode()?.and(0x3)?.coerceAtLeast(1) ?: 0)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, code, intent, flags)
    }

    /** Tapping the notification opens the quick-reply window (type + send), not the whole app. */
    private fun openAppIntent(slot: HourSlot): PendingIntent {
        val intent = Intent(context, QuickReplyActivity::class.java)
            .setAction(ACTION_QUICK_REPLY)
            .putExtra(EXTRA_DATE, slot.date.toString())
            .putExtra(EXTRA_HOUR, slot.hour)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            notificationId(slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun activityLabel(c: Context, a: Activity): String = if (!a.usesBuiltInName) a.name else when (a.nameKey) {
        "grind" -> c.getString(R.string.act_grind)
        "snack" -> c.getString(R.string.act_snack)
        "vibe" -> c.getString(R.string.act_vibe)
        "study" -> c.getString(R.string.act_study)
        "walk" -> c.getString(R.string.act_walk)
        "nap" -> c.getString(R.string.act_nap)
        "game" -> c.getString(R.string.act_game)
        "chat" -> c.getString(R.string.act_chat)
        "workout" -> c.getString(R.string.act_workout)
        else -> a.name
    }

    companion object {
        const val CHANNEL_CHECKINS = "checkins"
        const val CHANNEL_STATUS = "status"
        const val CHANNEL_REVIEW = "daily_review"

        /** Outside the per-hour id range (those are masked to 0x0FFFFFFF). */
        private const val REVIEW_NOTIFICATION_ID = 0x10000001
        const val ACTION_QUICK_REPLY = "com.timedoesntwait.action.QUICK_REPLY"
        const val EXTRA_DATE = "slot_date"
        const val EXTRA_HOUR = "slot_hour"
        private const val INK = 0xFF1B3693.toInt()
        private const val LOGGED_VISIBLE_MS = 4_000L

        /** Stable, unique id per hour: hours since 1970-01-01 (fits in an Int until year ~246,000). */
        fun notificationId(slot: HourSlot): Int = (slot.date.toEpochDays() * 24 + slot.hour).toInt() and 0x0FFFFFFF

        fun slotFrom(intent: Intent): HourSlot? {
            val date = intent.getStringExtra(EXTRA_DATE) ?: return null
            val hour = intent.getIntExtra(EXTRA_HOUR, -1).takeIf { it in 0..23 } ?: return null
            return runCatching { HourSlot(LocalDate.parse(date), hour) }.getOrNull()
        }
    }
}
