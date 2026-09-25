package com.threedreport.core.report

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogReportTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private fun product(name: String, salePrice: Double, cost: Double, minutes: Double, quantity: Int = 1, kind: QuoteKind = QuoteKind.PRODUCT) = SavedQuote(
        id = name,
        name = name,
        quote = Quote(
            prints = listOf(quotedPrint(PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = minutes))),
            costs = costsOf(cost),
            salePrice = salePrice,
            quantity = quantity,
        ),
        savedAtEpochMillis = 0L,
        kind = kind,
    )

    @Test
    fun productsAreRankedByProfitPerMachineHourAndOrdersStayOut() {
        val summary = CatalogReport.summarize(
            listOf(
                product("Vaso", salePrice = 60.0, cost = 20.0, minutes = 240.0), // 40 em 4 h = 10/h
                product("Chaveiro", salePrice = 10.0, cost = 4.0, minutes = 30.0), // 6 em 0,5 h = 12/h
                product("Sem tempo", salePrice = 5.0, cost = 1.0, minutes = 0.0),
                product("Pedido", salePrice = 999.0, cost = 1.0, minutes = 10.0, kind = QuoteKind.ORDER),
            ),
        )

        assertEquals(3, summary.productCount)
        assertEquals(listOf("Chaveiro", "Vaso", "Sem tempo"), summary.products.map { it.name })
        assertEquals(12.0, summary.products.first().profitPerPrintHour!!, 1e-9)
        assertNull(summary.products.last().profitPerPrintHour)
    }

    @Test
    fun priceIsTheSameTheCatalogPdfShowsWithServicesAndQuantity() {
        val kit = product("Kit", salePrice = 30.0, cost = 12.0, minutes = 60.0, quantity = 3)
            .copy(services = listOf(QuoteService(id = "pintura", name = "Pintura", price = 5.0, chargedPerOrder = true)))
        val summary = CatalogReport.summarize(listOf(kit, product("Vaso", salePrice = 60.0, cost = 20.0, minutes = 240.0)))

        val kitStat = summary.products.first { it.name == "Kit" }
        assertEquals(kit.totalWithServices, kitStat.price, 1e-9)
        assertEquals(35.0, kitStat.price, 1e-9)
        assertEquals(18.0, kitStat.profit, 1e-9)
        assertEquals(35.0, summary.minPrice!!, 1e-9)
        assertEquals(60.0, summary.maxPrice!!, 1e-9)
    }

    @Test
    fun emptyCatalog() {
        val summary = CatalogReport.summarize(emptyList())

        assertEquals(0, summary.productCount)
        assertNull(summary.minPrice)
        assertEquals(emptyList(), summary.products)
    }
}
