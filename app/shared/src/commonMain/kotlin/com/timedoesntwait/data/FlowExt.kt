package com.timedoesntwait.data

import app.cash.sqldelight.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.mapToOne as sqlMapToOne

internal inline fun <T, R> Flow<List<T>>.mapRows(crossinline transform: (T) -> R): Flow<List<R>> =
    map { rows -> rows.map(transform) }

internal fun <T : Any> Flow<Query<T>>.mapToOne(dispatcher: CoroutineDispatcher): Flow<T> = sqlMapToOne(dispatcher)
