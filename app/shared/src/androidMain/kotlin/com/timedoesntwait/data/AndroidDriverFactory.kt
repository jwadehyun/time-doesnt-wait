package com.timedoesntwait.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.timedoesntwait.db.AppDatabase

class AndroidDriverFactory(private val context: Context) : DriverFactory {
    override fun create(): SqlDriver = AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = context,
        name = DATABASE_NAME,
        callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                // WAL lets the notification receiver write while the UI reads.
                db.enableWriteAheadLogging()
            }
        },
    )
}
