package com.threedreport.core.report

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuoteReportTest {

    private fun quoteOf(filamentName: String, salePrice: Double, productionCost: Double): SavedQuote {
        val filament = Filament(id = filamentName, name = filamentName, pricePerKg = 100.0, densityGPerCm3 = 1.24)
        return SavedQuote(
            id = filamentName + salePrice,
            name = "Peça",
            quote = Quote(
                job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 10.0),
                filamentWeightGrams = 5.0,
                costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).copy(material = productionCost),
                productionCost = productionCost,
                salePrice = salePrice,
            ),
            savedAtEpochMillis = 0L,
        )
    }

    @Test
    fun emptyListSummarizesToZeroedSummary() {
        val summary = QuoteReport.summarize(emptyList())

        assertEquals(0, summary.quoteCount)
        assertEquals(0.0, summary.totalSalePrice)
        assertEquals(0.0, summary.totalProfit)
        assertNull(summary.mostUsedFilamentName)
        assertEquals(0, summary.mostUsedFilamentCount)
    }

    @Test
    fun sumsSalePriceAndProfitAcrossQuotes() {
        val quotes = listOf(
            quoteOf("PLA", salePrice = 20.0, productionCost = 10.0),
            quoteOf("PLA", salePrice = 30.0, productionCost = 15.0),
        )

        val summary = QuoteReport.summarize(quotes)

        assertEquals(2, summary.quoteCount)
        assertEquals(50.0, summary.totalSalePrice)
        assertEquals(25.0, summary.totalProfit)
    }

    @Test
    fun mostUsedFilamentIsTheOneWithMostQuotes() {
        val quotes = listOf(
            quoteOf("PLA", salePrice = 20.0, productionCost = 10.0),
            quoteOf("PLA", salePrice = 20.0, productionCost = 10.0),
            quoteOf("PETG", salePrice = 20.0, productionCost = 10.0),
        )

        val summary = QuoteReport.summarize(quotes)

        assertEquals("PLA", summary.mostUsedFilamentName)
        assertEquals(2, summary.mostUsedFilamentCount)
    }

    @Test
    fun includesServicesInTotalSalePrice() {
        val withServices = quoteOf("PLA", salePrice = 20.0, productionCost = 10.0)
            .copy(services = listOf(com.threedreport.core.model.Service(id = "s", name = "Pintura", price = 5.0)))

        val summary = QuoteReport.summarize(listOf(withServices))

        assertEquals(25.0, summary.totalSalePrice)
    }
}
