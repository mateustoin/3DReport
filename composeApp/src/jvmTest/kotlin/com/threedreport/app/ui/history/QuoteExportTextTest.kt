package com.threedreport.app.ui.history

import com.threedreport.core.model.CostBreakdown
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
}
