package com.threedreport.core.model

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Orçamento parado em Orçado (decisão 149, item 26). */
class SavedQuoteStaleTest {

    private val day = 86_400_000L

    /** Dia de um instante em UTC: nos testes o fuso não importa, quem roda o app passa o fuso local. */
    private val utcDay: (Long) -> Long = { it.floorDiv(day) }

    private fun quoted(savedAtDay: Long = 10) = SavedQuote(
        id = "1",
        name = "Peça",
        quote = Quote(
            prints = listOf(
                quotedPrint(
                    PrintJob(
                        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                        filamentLengthMeters = 1.0,
                        printTimeMinutes = 60.0,
                    ),
                ),
            ),
            costs = costsOf(),
            salePrice = 10.0,
        ),
        savedAtEpochMillis = savedAtDay * day + 1000,
        statusHistory = listOf(StatusChange(OrderStatus.ORCADO, savedAtDay * day + 1000)),
    )

    @Test
    fun daysCountFromCreation() {
        assertEquals(3, quoted(savedAtDay = 10).staleDays(todayEpochDay = 13, epochDayOf = utcDay))
        assertEquals(0, quoted(savedAtDay = 10).staleDays(todayEpochDay = 10, epochDayOf = utcDay))
    }

    @Test
    fun theMostRecentOfEditFollowUpAndBackToQuotedWins() {
        val base = quoted(savedAtDay = 10)

        assertEquals(1, base.copy(lastEditedEpochMillis = 12 * day).staleDays(13, utcDay))
        assertEquals(0, base.copy(lastFollowUpEpochMillis = 13 * day).staleDays(13, utcDay))
        val backToQuoted = base
            .withStatus(OrderStatus.APROVADO, 11 * day)
            .withStatus(OrderStatus.ORCADO, 12 * day)
        assertEquals(1, backToQuoted.staleDays(13, utcDay))
    }

    @Test
    fun onlyAQuotedOrderCanBeStale() {
        assertNull(quoted().withStatus(OrderStatus.APROVADO, 11 * day).staleDays(30, utcDay))
        assertNull(quoted().copy(kind = QuoteKind.PRODUCT).staleDays(30, utcDay))
    }

    @Test
    fun staleAfterTheThresholdAndZeroTurnsItOff() {
        val saved = quoted(savedAtDay = 10)

        assertFalse(saved.isStale(todayEpochDay = 12, thresholdDays = 3, epochDayOf = utcDay))
        assertTrue(saved.isStale(todayEpochDay = 13, thresholdDays = 3, epochDayOf = utcDay))
        assertFalse(saved.isStale(todayEpochDay = 100, thresholdDays = 0, epochDayOf = utcDay))

        assertNull(saved.staleDaysIfStale(todayEpochDay = 12, thresholdDays = 3, epochDayOf = utcDay))
        assertEquals(4, saved.staleDaysIfStale(todayEpochDay = 14, thresholdDays = 3, epochDayOf = utcDay))
        assertNull(saved.staleDaysIfStale(todayEpochDay = 100, thresholdDays = 0, epochDayOf = utcDay))
    }

    @Test
    fun aClockBackwardsNeverGivesNegativeDays() {
        assertEquals(0, quoted(savedAtDay = 10).staleDays(todayEpochDay = 5, epochDayOf = utcDay))
    }
}
