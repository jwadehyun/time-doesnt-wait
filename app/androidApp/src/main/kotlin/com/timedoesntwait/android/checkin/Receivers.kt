package com.timedoesntwait.android.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.domain.slotForPing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Runs [block] off the main thread while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } catch (t: Throwable) {
            Log.e("CheckinReceiver", "Receiver failed", t)
        } finally {
            pending.finish()
        }
    }
}

/** Fires at H:00. Posts the check-in for hour H−1 and schedules the next ping. */
class CheckinAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: CheckinScheduler by inject()
    private val notifier: CheckinNotifier by inject()
    private val journal: JournalRepository by inject()
    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PING) return
        val pingAt = intent.getLongExtra(EXTRA_PING_AT, 0L)
        runAsync {
            try {
                if (pingAt > 0) {
                    // Record how late the alarm fired; > 5 min shows the battery-saver tip in the app.
                    val lateMin = ((System.currentTimeMillis() - pingAt) / 60_000).toInt().coerceAtLeast(0)
                    settings.lastAlarmLatenessMinutes = lateMin
                    val slot = slotForPing(CheckinScheduler.pingTime(pingAt))
                    // Skip if the hour was already logged in the app before the ping.
                    if (journal.entryFor(slot) == null) notifier.showCheckin(slot)
                }
            } finally {
                scheduler.scheduleNext()
            }
        }
    }

    companion object {
        const val ACTION_PING = "com.timedoesntwait.action.PING"
        const val EXTRA_PING_AT = "ping_at"
    }
}

/** Handles the inline reply and the one-tap activity buttons. Works from the lock screen. */
class CheckinReplyReceiver : BroadcastReceiver(), KoinComponent {
    private val notifier: CheckinNotifier by inject()
    private val journal: JournalRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        // The hour comes from the notification itself, so a late reply still logs the right hour.
        val slot = CheckinNotifier.slotFrom(intent) ?: return
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY_TEXT)?.toString()?.trim()
        val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID)
        runAsync {
            val entry = when {
                intent.action == ACTION_REPLY && !text.isNullOrEmpty() ->
                    journal.logText(slot, text, EntrySource.NOTIFICATION_REPLY)
                intent.action == ACTION_QUICK && activityId != null ->
                    journal.logActivity(slot, activityId, EntrySource.QUICK_BUTTON)
                else -> null
            }
            if (entry == null) {
                notifier.showCheckin(slot) // empty reply: restore the notification so the spinner stops
                return@runAsync
            }
            val activity = entry.activityId?.let { id -> journal.allActivities().first().firstOrNull { it.id == id } }
            notifier.showLogged(slot, activity, activityMissing = entry.activityId != null && activity == null)
        }
    }

    companion object {
        const val ACTION_REPLY = "com.timedoesntwait.action.REPLY"
        const val ACTION_QUICK = "com.timedoesntwait.action.QUICK"
        const val KEY_REPLY_TEXT = "reply_text"
        const val EXTRA_ACTIVITY_ID = "activity_id"
    }
}

/** Rebuilds the next alarm after reboot, update, clock or zone change, or a permission change. */
class RescheduleReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: CheckinScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> runAsync { scheduler.scheduleIfOnboarded() }
        }
    }
}
