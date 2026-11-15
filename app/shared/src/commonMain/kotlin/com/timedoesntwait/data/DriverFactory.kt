package com.timedoesntwait.data

import app.cash.sqldelight.db.SqlDriver

const val DATABASE_NAME = "timedoesntwait.db"

/** Platform-specific SQLite driver. Android: AndroidSqliteDriver. JVM (tests): in-memory JDBC driver. */
fun interface DriverFactory {
    fun create(): SqlDriver
}
