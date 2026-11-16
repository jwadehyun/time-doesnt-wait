package com.timedoesntwait.android.checkin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.timedoesntwait.android.AndroidPlatform
import com.timedoesntwait.android.MainActivity
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.domain.HourSlot
import com.timedoesntwait.ui.screens.QuickReplyPopup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Opens when a check-in notification is tapped: a small typing window over whatever was on screen,
 * so the hour can be logged like replying to a message, without opening the full app.
 * Works the same on every phone, unlike inline notification replies, which some phones hide.
 */
class QuickReplyActivity : ComponentActivity() {
    private val notifier: CheckinNotifier by inject()
    private val journal: JournalRepository by inject()
    private val platform: AndroidPlatform by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        platform.applyStoredLanguage()
        val slot = CheckinNotifier.slotFrom(intent)
        if (slot == null) {
            finish()
            return
        }
        show(slot)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CheckinNotifier.slotFrom(intent)?.let { setIntent(intent); show(it) }
    }

    private fun show(slot: HourSlot) {
        setContent {
            QuickReplyPopup(
                slot = slot,
                onLogged = { entry ->
                    lifecycleScope.launch {
                        val activity = entry.activityId?.let { id -> journal.allActivities().first().firstOrNull { it.id == id } }
                        notifier.showLogged(slot, activity, activityMissing = entry.activityId != null && activity == null)
                        finish()
                    }
                },
                onMoreDetails = {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .setAction(MainActivity.ACTION_OPEN_SLOT)
                            .putExtra(CheckinNotifier.EXTRA_DATE, slot.date.toString())
                            .putExtra(CheckinNotifier.EXTRA_HOUR, slot.hour)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    )
                    finish()
                },
                onDismiss = { finish() },
            )
        }
    }
}
