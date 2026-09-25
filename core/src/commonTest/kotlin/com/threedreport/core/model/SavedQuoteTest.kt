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
}
