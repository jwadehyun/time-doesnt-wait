package com.timedoesntwait.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timedoesntwait.db.AppDatabase

/** In-memory database used by tests. */
class JvmDriverFactory : DriverFactory {
    override fun create(): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
}
