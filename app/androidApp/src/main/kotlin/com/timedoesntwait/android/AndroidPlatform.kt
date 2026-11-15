package com.timedoesntwait.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timedoesntwait.android.checkin.CheckinNotifier
import com.timedoesntwait.android.checkin.CheckinScheduler
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.domain.AppLanguage
import com.timedoesntwait.platform.PermissionState
import com.timedoesntwait.platform.PermissionStatus
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.platform.SetupPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class AndroidPlatform(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scheduler: CheckinScheduler,
) : Platform {

    /** Set by MainActivity so the runtime notification permission dialog can be shown. */
    var requestNotificationPermission: (() -> Unit)? = null

    private val systemLocale: Locale = Locale.getDefault()

    private val _permissions = MutableStateFlow(readPermissions())
    override val permissions: StateFlow<PermissionState> = _permissions

    private val _language = MutableStateFlow(currentLanguage())
    override val language: StateFlow<AppLanguage> = _language

    override val manufacturer: String = Build.MANUFACTURER ?: ""

    override val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    override fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    override fun rescheduleCheckins() = scheduler.scheduleIfOnboarded()

    override fun requestPermission(permission: SetupPermission) {
        val pkg = Uri.fromParts("package", context.packageName, null)
        when (permission) {
            SetupPermission.NOTIFICATIONS -> {
                val canAsk = Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                val launcher = requestNotificationPermission
                if (canAsk && launcher != null && !askedNotificationsBefore()) {
                    settings.put(KEY_ASKED_NOTIFICATIONS, "true")
                    launcher()
                } else {
                    open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            SetupPermission.EXACT_ALARMS -> if (Build.VERSION.SDK_INT >= 31) {
                open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            }
            SetupPermission.BATTERY -> {
                @SuppressLint("BatteryLife") // Personal install; Play review would need a justification.
                val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
                if (!open(direct)) open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            SetupPermission.LOCK_SCREEN -> {
                if (!open(Intent("android.settings.LOCK_SCREEN_SETTINGS"))) {
                    open(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }

    private fun askedNotificationsBefore() = settings.get(KEY_ASKED_NOTIFICATIONS) == "true"

    private fun open(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun readPermissions(): PermissionState {
        val nm = NotificationManagerCompat.from(context)
        val channelOn = nm.getNotificationChannel(CheckinNotifier.CHANNEL_CHECKINS)
            ?.let { it.importance != NotificationManager.IMPORTANCE_NONE } ?: true
        val notifications = if (nm.areNotificationsEnabled() && channelOn) PermissionStatus.GRANTED else PermissionStatus.NEEDED

        val alarms = if (Build.VERSION.SDK_INT >= 31) {
            val am = context.getSystemService(AlarmManager::class.java)
            if (am.canScheduleExactAlarms()) PermissionStatus.GRANTED else PermissionStatus.NEEDED
        } else PermissionStatus.NOT_APPLICABLE

        val pm = context.getSystemService(PowerManager::class.java)
        val battery = if (pm.isIgnoringBatteryOptimizations(context.packageName)) PermissionStatus.GRANTED else PermissionStatus.NEEDED

        // Our notification is VISIBILITY_PUBLIC, so only the "show notifications on lock screen"
        // switch matters, not "hide sensitive content".
        val lockShows = Settings.Secure.getInt(context.contentResolver, "lock_screen_show_notifications", 1) == 1
        val lock = if (lockShows) PermissionStatus.GRANTED else PermissionStatus.NEEDED

        return PermissionState(
            mapOf(
                SetupPermission.NOTIFICATIONS to notifications,
                SetupPermission.EXACT_ALARMS to alarms,
                SetupPermission.BATTERY to battery,
                SetupPermission.LOCK_SCREEN to lock,
            ),
        )
    }

    // ── Language ─────────────────────────────────────────────

    override fun setLanguage(language: AppLanguage?) {
        settings.language = language
        applyStoredLanguage()
    }

    /** Applies the stored choice to the process default locale, which Compose resources read. */
    fun applyStoredLanguage() {
        val chosen = settings.language
        val locale = chosen?.let { Locale.forLanguageTag(it.tag) } ?: systemLocale
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))
        _language.value = chosen ?: AppLanguage.fromTag(systemLocale.language)
    }

    private fun currentLanguage(): AppLanguage =
        settings.language ?: AppLanguage.fromTag(Locale.getDefault().language)

    /** A context whose resources use the app language, for notifications built outside the UI. */
    fun localizedContext(): Context {
        val locale = Locale.forLanguageTag(_language.value.tag)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config)
    }

    companion object {
        private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
