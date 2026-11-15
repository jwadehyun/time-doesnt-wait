package com.timedoesntwait.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

enum class AppLanguage(val tag: String) {
    EN("en"), KO("ko");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            if (tag != null && tag.lowercase().startsWith("ko")) KO else EN
    }
}

/** Formats hours and dates the way the design doc specifies: "2–3 PM" / "오후 2–3시". */
object TimeLabels {

    fun hourRange(hour: Int, lang: AppLanguage): String {
        val end = (hour + 1) % 24
        return when (lang) {
            AppLanguage.EN -> {
                val startHalf = if (hour < 12) "AM" else "PM"
                val endHalf = if (end < 12) "AM" else "PM"
                if (startHalf == endHalf) "${h12(hour)}–${h12(end)} $endHalf"
                else "${h12(hour)} $startHalf–${h12(end)} $endHalf"
            }
            AppLanguage.KO -> {
                val startHalf = if (hour < 12) "오전" else "오후"
                val endHalf = if (end < 12) "오전" else "오후"
                if (startHalf == endHalf) "$startHalf ${h12(hour)}–${h12(end)}시"
                else "$startHalf ${h12(hour)}시–$endHalf ${h12(end)}시"
            }
        }
    }

    fun clock(hour: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.EN -> "${h12(hour)}:00 ${if (hour < 12) "AM" else "PM"}"
        AppLanguage.KO -> "${if (hour < 12) "오전" else "오후"} ${h12(hour)}:00"
    }

    fun date(date: LocalDate, lang: AppLanguage): String = when (lang) {
        AppLanguage.EN -> "${weekdayShort(date.dayOfWeek, lang)}, ${MONTHS_EN[date.month.ordinal]} ${date.day}"
        AppLanguage.KO -> "${date.month.ordinal + 1}월 ${date.day}일 (${weekdayShort(date.dayOfWeek, lang)})"
    }

    fun weekdayShort(day: DayOfWeek, lang: AppLanguage): String = when (lang) {
        AppLanguage.EN -> WEEKDAYS_EN[day.ordinal]
        AppLanguage.KO -> WEEKDAYS_KO[day.ordinal]
    }

    private fun h12(hour: Int): Int = if (hour % 12 == 0) 12 else hour % 12

    private val MONTHS_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val WEEKDAYS_EN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val WEEKDAYS_KO = listOf("월", "화", "수", "목", "금", "토", "일")
}
