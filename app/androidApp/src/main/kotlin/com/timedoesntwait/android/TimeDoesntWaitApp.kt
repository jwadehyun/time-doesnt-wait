package com.timedoesntwait.android

import android.app.Application
import android.content.res.Configuration
import com.timedoesntwait.android.checkin.CheckinNotifier
import com.timedoesntwait.android.checkin.CheckinScheduler
import com.timedoesntwait.data.AndroidDriverFactory
import com.timedoesntwait.data.JournalRepository
import com.timedoesntwait.data.ReviewRepository
import com.timedoesntwait.data.ScheduleRepository
import com.timedoesntwait.data.SettingsRepository
import com.timedoesntwait.data.sharedModule
import com.timedoesntwait.domain.EntrySource
import com.timedoesntwait.platform.Platform
import com.timedoesntwait.ui.AppState
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get

class TimeDoesntWaitApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TimeDoesntWaitApp)
            modules(
                sharedModule(AndroidDriverFactory(this@TimeDoesntWaitApp)),
                module {
                    single { CheckinScheduler(get(), get(), get()) }
                    single { CheckinNotifier(get(), get(), get()) }
                    single { AndroidPlatform(get(), get(), get()) }
                    single<Platform> { get<AndroidPlatform>() }
                    // Lives in the Application so screen state survives activity recreation.
                    single { AppState(onboarded = get<SettingsRepository>().onboardingDone) }
                },
            )
        }

        val journal = get<JournalRepository>(JournalRepository::class.java)
        val schedule = get<ScheduleRepository>(ScheduleRepository::class.java)
        val notifier = get<CheckinNotifier>(CheckinNotifier::class.java)
        val platform = get<AndroidPlatform>(AndroidPlatform::class.java)

        platform.applyStoredLanguage()
        journal.seedDefaultsIfNeeded()
        schedule.seedIfEmpty()
        notifier.ensureChannels()

        // Logging an hour from inside the app clears that hour's notification. Replies from the
        // notification itself update it to "Logged ✓" instead (see CheckinReplyReceiver).
        journal.onSlotLogged = { slot, source ->
            if (source != EntrySource.NOTIFICATION_REPLY && source != EntrySource.QUICK_BUTTON) notifier.cancel(slot)
        }

        // Reviewing the day in the app clears the evening reminder.
        get<ReviewRepository>(ReviewRepository::class.java).onReviewSaved = { notifier.cancelReviewReminder() }

        get<CheckinScheduler>(CheckinScheduler::class.java).scheduleIfOnboarded()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A system language change resets the default locale; keep the language the user picked.
        get<AndroidPlatform>(AndroidPlatform::class.java).applyStoredLanguage()
    }
}
