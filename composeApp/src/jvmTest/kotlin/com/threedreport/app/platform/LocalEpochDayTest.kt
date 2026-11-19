package com.threedreport.app.platform

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** O dia de um instante no fuso do computador, pra contar os dias de orçamento parado (decisão 149). */
class LocalEpochDayTest {

    @Test
    fun theFirstAndLastMomentOfADayAreTheSameDay() {
        val day = LocalDate.of(2026, 11, 17)
        val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        assertEquals(day.toEpochDay(), localEpochDayOf(start))
        assertEquals(day.toEpochDay(), localEpochDayOf(end))
        assertEquals(day.toEpochDay() + 1, localEpochDayOf(end + 1))
    }
}
