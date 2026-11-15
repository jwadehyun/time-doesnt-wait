package com.timedoesntwait.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeLabelsTest {
    @Test
    fun hourRanges() {
        assertEquals("2–3 PM", TimeLabels.hourRange(14, AppLanguage.EN))
        assertEquals("11 AM–12 PM", TimeLabels.hourRange(11, AppLanguage.EN))
        assertEquals("11 PM–12 AM", TimeLabels.hourRange(23, AppLanguage.EN))
        assertEquals("오후 2–3시", TimeLabels.hourRange(14, AppLanguage.KO))
        assertEquals("오전 11시–오후 12시", TimeLabels.hourRange(11, AppLanguage.KO))
    }

    @Test
    fun dates() {
        assertEquals("Fri, Nov 13", TimeLabels.date(LocalDate(2026, 11, 13), AppLanguage.EN))
        assertEquals("11월 13일 (금)", TimeLabels.date(LocalDate(2026, 11, 13), AppLanguage.KO))
    }

    @Test
    fun uuidV7Shape() {
        val id = Ids.uuidV7(1_794_640_804_050)
        assertEquals(36, id.length)
        assertEquals('7', id[14])
        assertEquals("01a1d8ed-78d2", id.substring(0, 13))
    }
}
