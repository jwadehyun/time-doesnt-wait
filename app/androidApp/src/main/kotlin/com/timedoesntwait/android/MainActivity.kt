package com.timedoesntwait.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.timedoesntwait.android.checkin.CheckinNotifier
import com.timedoesntwait.android.checkin.CheckinScheduler
import com.timedoesntwait.ui.App
import com.timedoesntwait.ui.AppState
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val appState: AppState by inject()
    private val platform: AndroidPlatform by inject()
    private val scheduler: CheckinScheduler by inject()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { platform.refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        platform.applyStoredLanguage()

        if (Build.VERSION.SDK_INT >= 33) {
            platform.requestNotificationPermission = {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!appState.back()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
        handleIntent(intent)
        setContent { App(appState) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed in system settings while we were away.
        platform.refreshPermissions()
        scheduler.scheduleIfOnboarded()
    }

    override fun onDestroy() {
        platform.requestNotificationPermission = null
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_SLOT) return
        CheckinNotifier.slotFrom(intent)?.let { appState.openQuickLog(it) }
    }

    companion object {
        const val ACTION_OPEN_SLOT = "com.timedoesntwait.action.OPEN_SLOT"
    }
}
