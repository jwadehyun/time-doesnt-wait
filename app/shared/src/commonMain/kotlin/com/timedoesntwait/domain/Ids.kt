package com.timedoesntwait.domain

import kotlin.random.Random

/** UUIDv7: 48-bit Unix-millis timestamp + random bits, so ids sort by creation time. */
object Ids {
    private val random = Random.Default

    fun uuidV7(nowMillis: Long): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        for (i in 0 until 6) bytes[i] = (nowMillis ushr (8 * (5 - i))).toByte()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte() // version 7
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // RFC 4122 variant
        val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20)}"
    }
}
