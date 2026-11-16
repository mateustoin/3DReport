package com.threedreport.app.platform

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Datas e números saem de um ponto só, o [AppLocale] (Leva 10B, parte 7). Na 2.2 ele continua português do Brasil,
 * e nada muda no que aparece: estes são os textos de antes.
 */
class AppLocaleTest {

    @Test
    fun theAppSpeaksBrazilianPortuguese() {
        assertEquals("pt-BR", AppLocale.languageTag)
        assertEquals(',', AppLocale.decimalSeparator)
    }

    @Test
    fun datesLookTheSameAsBefore() {
        val day = LocalDate.of(2026, 9, 30).toEpochDay()
        val instant = LocalDateTime.of(2026, 9, 24, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals("30/09/2026", formatDate(day))
        assertEquals("30/09", formatShortDate(day))
        assertEquals("24/09/2026 14:30", formatDateTime(instant))
        assertEquals("quarta", weekdayName(day))
        assertEquals("sexta", weekdayName(LocalDate.of(2026, 10, 2).toEpochDay()))
    }
}
