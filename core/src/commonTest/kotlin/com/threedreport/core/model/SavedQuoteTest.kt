package com.threedreport.core.model

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedQuoteTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private fun savedQuoteOf(
        status: OrderStatus = OrderStatus.ORCADO,
        deliveryDateEpochDay: Long? = null,
        printTimeMinutes: Double = 90.0,
        quantity: Int = 1,
    ) = SavedQuote(
        id = "1",
        name = "Peça",
        quote = Quote(
            prints = listOf(quotedPrint(PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = printTimeMinutes))),
            costs = costsOf(),
            salePrice = 10.0,
            quantity = quantity,
        ),
        savedAtEpochMillis = 0L,
        status = status,
        deliveryDateEpochDay = deliveryDateEpochDay,
    )

    @Test
    fun overdueOnlyAfterTheDeadlineAndBeforeDelivery() {
        val deadline = 100L

        assertFalse(savedQuoteOf(OrderStatus.APROVADO, deadline).isDeliveryOverdue(todayEpochDay = 100L), "no próprio dia ainda está no prazo")
        assertTrue(savedQuoteOf(OrderStatus.APROVADO, deadline).isDeliveryOverdue(todayEpochDay = 101L))
        assertTrue(savedQuoteOf(OrderStatus.ORCADO, deadline).isDeliveryOverdue(todayEpochDay = 101L))
        assertFalse(savedQuoteOf(OrderStatus.ENTREGUE, deadline).isDeliveryOverdue(todayEpochDay = 101L))
        assertFalse(savedQuoteOf(OrderStatus.APROVADO, null).isDeliveryOverdue(todayEpochDay = 101L))
    }

    @Test
    fun totalPrintTimeMultipliesByQuantity() {
        assertEquals(270.0, savedQuoteOf(printTimeMinutes = 90.0, quantity = 3).totalPrintTimeMinutes)
    }

    @Test
    fun savedQuoteIsAnOrderByDefault() {
        val saved = savedQuoteOf()

        assertEquals(QuoteKind.ORDER, saved.kind)
        assertTrue(saved.isOrder)
    }

    @Test
    fun productIsNeverOverdue() {
        val product = savedQuoteOf(OrderStatus.ORCADO, deliveryDateEpochDay = 100L).copy(kind = QuoteKind.PRODUCT)

        assertFalse(product.isOrder)
        assertFalse(product.isDeliveryOverdue(todayEpochDay = 101L))
    }

    @Test
    fun cancelledIsNeitherSoldNorOpenNorOverdue() {
        val cancelled = savedQuoteOf(OrderStatus.CANCELADO, deliveryDateEpochDay = 100L)

        assertFalse(cancelled.status.isSold)
        assertFalse(cancelled.status.isOpen)
        assertFalse(cancelled.isDeliveryOverdue(todayEpochDay = 101L))
    }

    @Test
    fun statusChangesAreRecordedWithTheirMoment() {
        val saved = savedQuoteOf().withStatus(OrderStatus.APROVADO, atEpochMillis = 500L).withStatus(OrderStatus.EM_IMPRESSAO, 700L)

        assertEquals(listOf(StatusChange(OrderStatus.APROVADO, 500L), StatusChange(OrderStatus.EM_IMPRESSAO, 700L)), saved.statusHistory)
        assertEquals(saved, saved.withStatus(OrderStatus.EM_IMPRESSAO, 900L), "o mesmo status não vira uma mudança nova")
    }

    @Test
    fun saleDateIsWhenTheClientClosedNotWhenTheQuoteWasCreated() {
        val saved = savedQuoteOf().withStatus(OrderStatus.APROVADO, 500L).withStatus(OrderStatus.PRONTO, 900L)

        assertEquals(500L, saved.soldAtEpochMillis, "andar dentro do fluxo de venda não muda a data da venda")
        assertEquals(900L, saved.printedAtEpochMillis)
        assertEquals(null, savedQuoteOf().soldAtEpochMillis, "orçado não é venda")
    }

    @Test
    fun reopeningAndClosingAgainMovesTheSaleDate() {
        val saved = savedQuoteOf()
            .withStatus(OrderStatus.APROVADO, 500L)
            .withStatus(OrderStatus.ORCADO, 600L)
            .withStatus(OrderStatus.APROVADO, 800L)

        assertEquals(800L, saved.soldAtEpochMillis)
    }

    @Test
    fun orderSavedAlreadySoldWithoutHistoryUsesTheCreationDate() {
        assertEquals(0L, savedQuoteOf(OrderStatus.APROVADO).soldAtEpochMillis)
    }

    @Test
    fun displayNumberIsPaddedAndMissingWithoutNumber() {
        assertEquals("#0042", savedQuoteOf().copy(number = 42).displayNumber)
        assertEquals(null, savedQuoteOf().displayNumber)
    }

    @Test
    fun printSettingsLiveOnTheFirstPrint() {
        val settings = PrintSettings(layerHeightMm = 0.2, infillPercentage = 15.0)
        val saved = savedQuoteOf().withPrintSettings(settings)

        assertEquals(settings, saved.printSettings)
        assertEquals(settings, saved.quote.prints.first().job.settings)
        assertEquals(null, saved.withPrintSettings(PrintSettings()).printSettings, "configuração vazia não é guardada")
    }

    @Test
    fun theSnapshotKeepsTheFilamentButNotItsStock() {
        val withColors = filament.copy(colors = listOf(FilamentColor(id = "red", inStock = false), FilamentColor(id = "blue")))
        val usage = FilamentUsage(withColors, lengthMeters = 1.0)

        assertEquals(FilamentSnapshot.of(withColors), usage.filament)
        assertEquals(withColors.weightGrams(1.0), usage.weightGrams, 1e-12)
    }
}
