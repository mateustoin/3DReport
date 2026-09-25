package com.threedreport.core.model

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuoteServiceTest {

    @Test
    fun perPieceServiceMultipliesByQuantity() {
        val paint = QuoteService(id = "paint", name = "Pintura", price = 25.0, chargedPerOrder = false)

        assertEquals(250.0, paint.total(quantity = 10))
    }

    @Test
    fun perOrderServiceIsChargedOnce() {
        val delivery = QuoteService(id = "delivery", name = "Entrega", price = 15.0, chargedPerOrder = true)

        assertEquals(15.0, delivery.total(quantity = 10))
    }

    @Test
    fun savedQuoteTotalMixesPerPieceAndPerOrderServices() {
        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val savedQuote = SavedQuote(
            id = "q",
            name = "Chaveiros",
            quote = Quote(
                prints = listOf(quotedPrint(PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 10.0))),
                costs = costsOf(50.0),
                salePrice = 100.0,
                quantity = 10,
            ),
            services = listOf(
                QuoteService(id = "paint", name = "Pintura", price = 2.0, chargedPerOrder = false),
                QuoteService(id = "delivery", name = "Entrega", price = 15.0, chargedPerOrder = true),
            ),
            savedAtEpochMillis = 0L,
            shippingCost = 5.0,
        )

        // 100 de venda + 2 × 10 de pintura + 15 de entrega + 5 de frete.
        assertEquals(140.0, savedQuote.totalWithServices)
    }

    @Test
    fun catalogServiceAcceptsNoSuggestedPrice() {
        val service = Service(id = "paint", name = "Pintura")

        assertEquals(null, service.suggestedPrice)
        assertEquals(false, service.chargedPerOrder)
    }

    @Test
    fun negativePriceIsRejected() {
        assertFailsWith<IllegalArgumentException> { Service(id = "s", name = "Pintura", suggestedPrice = -1.0) }
        assertFailsWith<IllegalArgumentException> { QuoteService(id = "s", name = "Pintura", price = -1.0, chargedPerOrder = false) }
    }
}
