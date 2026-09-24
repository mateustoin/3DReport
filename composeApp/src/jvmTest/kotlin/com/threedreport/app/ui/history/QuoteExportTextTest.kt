package com.threedreport.app.ui.history

import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoteExportTextTest {

    private val savedQuote = SavedQuote(
        id = "1",
        name = "Suporte de celular",
        quote = Quote(
            job = PrintJob(
                filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                filamentLengthMeters = 12.0,
                printTimeMinutes = 190.0,
            ),
            filamentWeightGrams = 35.79,
            costs = CostBreakdown(3.58, 1.48, 0.54, 0.36, 0.36, 1.78, 0.0),
            productionCost = 8.09,
            salePrice = 16.19,
        ),
        sourceLink = "https://example.com/model",
        savedAtEpochMillis = 0L,
        client = Client(name = "Maria Cliente", contact = "maria@example.com"),
    )

    @Test
    fun includesNameAndSalePriceOnly() {
        val text = savedQuote.toCopyPasteText()

        assertEquals("Suporte de celular\nVenda: R$ 16,19", text)
    }

    @Test
    fun neverIncludesInternalSourceLink() {
        val text = savedQuote.toCopyPasteText()

        assertFalse(text.contains(savedQuote.sourceLink!!))
    }

    @Test
    fun neverIncludesInternalClient() {
        val text = savedQuote.toCopyPasteText()

        assertFalse(text.contains(savedQuote.client!!.name))
    }

    @Test
    fun neverIncludesProductionCostOrProfit() {
        val text = savedQuote.toCopyPasteText()

        assertFalse(text.contains(savedQuote.quote.productionCost.toString()))
        assertFalse(text.contains("Lucro"))
        assertFalse(text.contains("Produção"))
    }

    @Test
    fun includesEachServiceAndTheGrandTotalWhenPresent() {
        val withServices = savedQuote.copy(
            services = listOf(Service(id = "paint", name = "Pintura", price = 20.0)),
        )

        val text = withServices.toCopyPasteText()

        assertEquals("Suporte de celular\nVenda: R$ 16,19\nPintura: R$ 20,00\nTotal: R$ 36,19", text)
    }

    @Test
    fun noTotalLineWhenThereAreNoServices() {
        val text = savedQuote.toCopyPasteText()

        assertFalse(text.contains("Total"))
        assertTrue(text.contains("Venda"))
    }

    @Test
    fun usesTheGivenCurrencyInsteadOfBrl() {
        val text = savedQuote.toCopyPasteText(Currency.USD)

        assertEquals("Suporte de celular\nVenda: $ 16.19", text)
    }

    @Test
    fun includesQuantityUnitPriceAndMultipliedServiceWhenQuantityIsGreaterThanOne() {
        val withQuantity = savedQuote.copy(
            quote = savedQuote.quote.copy(salePrice = 60.20, quantity = 10),
            services = listOf(Service(id = "paint", name = "Pintura", price = 15.0)),
        )

        val text = withQuantity.toCopyPasteText()

        // O "cada" fecha o texto e sai do total que o cliente paga (R$ 210,20 / 10), não do valor
        // de venda: é o mesmo número que a tela de Orçamento mostra pro mesmo orçamento.
        assertEquals(
            "Suporte de celular\nVenda: R$ 60,20\nPintura (× 10): R$ 150,00\nTotal: R$ 210,20\n10 peças · R$ 21,02 cada",
            text,
        )
        // A soma exibida bate com o total de fato cobrado (venda + serviços × quantidade).
        assertEquals(withQuantity.totalWithServices, 60.20 + 150.0, 0.001)
    }

    @Test
    fun includesShippingAndTotalEvenWithNoServices() {
        val withShipping = savedQuote.copy(shippingCost = 12.0)

        val text = withShipping.toCopyPasteText()

        assertEquals("Suporte de celular\nVenda: R$ 16,19\nFrete: R$ 12,00\nTotal: R$ 28,19", text)
        assertEquals(withShipping.totalWithServices, 16.19 + 12.0, 0.001)
    }

    @Test
    fun includesShippingAfterServicesAndBeforeTotalWhenQuantityIsGreaterThanOne() {
        val withShippingAndServices = savedQuote.copy(
            quote = savedQuote.quote.copy(salePrice = 60.20, quantity = 10),
            services = listOf(Service(id = "paint", name = "Pintura", price = 15.0)),
            shippingCost = 25.0,
        )

        val text = withShippingAndServices.toCopyPasteText()

        assertEquals(
            "Suporte de celular\nVenda: R$ 60,20\nPintura (× 10): R$ 150,00\nFrete: R$ 25,00\nTotal: R$ 235,20\n" +
                "10 peças · R$ 23,52 cada",
            text,
        )
        assertEquals(withShippingAndServices.totalWithServices, 60.20 + 150.0 + 25.0, 0.001)
    }

    @Test
    fun quantityOneKeepsTheOutputExactlyAsBeforeQuantityExisted() {
        val quantityOne = savedQuote.copy(
            services = listOf(Service(id = "paint", name = "Pintura", price = 20.0)),
        )
        check(quantityOne.quote.quantity == 1)

        val text = quantityOne.toCopyPasteText()

        assertEquals("Suporte de celular\nVenda: R$ 16,19\nPintura: R$ 20,00\nTotal: R$ 36,19", text)
        assertFalse(text.contains("peças"))
        assertFalse(text.contains("cada"))
        assertFalse(text.contains("×"))
    }

    private val september30 = java.time.LocalDate.of(2026, 9, 30).toEpochDay()

    @Test
    fun deliveryDateClosesTheMessage() {
        val withDeadline = savedQuote.copy(deliveryDateEpochDay = september30)

        assertEquals("Suporte de celular\nVenda: R$ 16,19\nPrazo de entrega: até 30/09/2026", withDeadline.toCopyPasteText())
    }

    @Test
    fun printTimeAppearsOnlyWhenTurnedOnAndCoversTheWholeOrder() {
        val twoPieces = savedQuote.copy(
            quote = savedQuote.quote.copy(quantity = 2, salePrice = 32.38),
            deliveryDateEpochDay = september30,
        )

        assertFalse(twoPieces.toCopyPasteText().contains("Tempo"))
        assertEquals(
            "Suporte de celular\nVenda: R$ 32,38\n2 peças · R$ 16,19 cada\nPrazo de entrega: até 30/09/2026\n" +
                "Tempo de impressão: 6 h 20 min (2 peças)",
            twoPieces.toCopyPasteText(showPrintTime = true),
        )
    }

    @Test
    fun singlePiecePrintTimeHasNoPieceCount() {
        assertEquals(
            "Suporte de celular\nVenda: R$ 16,19\nTempo de impressão: 3 h 10 min",
            savedQuote.toCopyPasteText(showPrintTime = true),
        )
    }
}
