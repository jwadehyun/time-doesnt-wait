package com.timedoesntwait.data

import com.timedoesntwait.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import kotlin.time.Clock

fun sharedModule(driverFactory: DriverFactory) = module {
    single { AppDatabase(driverFactory.create()) }
    single<Clock> { Clock.System }
    single { JournalRepository(get(), get(), Dispatchers.IO) }
    single { ScheduleRepository(get(), get(), Dispatchers.IO) }
    single { SettingsRepository(get(), Dispatchers.IO) }
}
