package com.threedreport.core.report

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuoteReportTest {

    private fun quoteOf(
        filamentName: String,
        salePrice: Double,
        productionCost: Double,
        status: OrderStatus = OrderStatus.APROVADO,
        name: String = "Peça",
        printTimeMinutes: Double = 10.0,
    ): SavedQuote {
        val filament = Filament(id = filamentName, name = filamentName, pricePerKg = 100.0, densityGPerCm3 = 1.24)
        return SavedQuote(
            id = filamentName + salePrice,
            name = name,
            status = status,
            quote = Quote(
                prints = listOf(quotedPrint(PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = printTimeMinutes))),
                costs = costsOf(productionCost),
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
    fun negotiatedDiscountIsSummedNetOfPricesAboveTheTable() {
        val plain = quoteOf("PLA", salePrice = 20.0, productionCost = 10.0)
        val discounted = quoteOf("PLA", salePrice = 25.0, productionCost = 10.0)
            .let { it.copy(quote = it.quote.copy(tableSalePrice = 30.0)) }
        val above = quoteOf("PLA", salePrice = 32.0, productionCost = 10.0)
            .let { it.copy(quote = it.quote.copy(tableSalePrice = 30.0)) }

        val summary = QuoteReport.summarize(listOf(plain, discounted, above))

        assertEquals(2, summary.negotiatedCount)
        assertEquals(3.0, summary.totalNegotiatedDiscount, 1e-9)
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
            .copy(services = listOf(com.threedreport.core.model.QuoteService(id = "s", name = "Pintura", price = 5.0, chargedPerOrder = false)))

        val summary = QuoteReport.summarize(listOf(withServices))

        assertEquals(25.0, summary.totalSalePrice)
    }

    @Test
    fun openQuotesStayOutOfSalesAndFeedConversion() {
        val quotes = listOf(
            quoteOf("PLA", salePrice = 20.0, productionCost = 10.0),
            quoteOf("PLA", salePrice = 50.0, productionCost = 10.0, status = OrderStatus.ORCADO),
            quoteOf("PLA", salePrice = 30.0, productionCost = 10.0, status = OrderStatus.ORCADO),
            quoteOf("PLA", salePrice = 40.0, productionCost = 10.0, status = OrderStatus.ENTREGUE),
        )

        val summary = QuoteReport.summarize(quotes)

        assertEquals(2, summary.quoteCount)
        assertEquals(60.0, summary.totalSalePrice)
        assertEquals(40.0, summary.totalProfit)
        assertEquals(2, summary.openQuoteCount)
        assertEquals(80.0, summary.openQuoteTotal)
        assertEquals(0.5, summary.conversionRate)
    }

    @Test
    fun onlyOpenQuotesMeansNoSalesButStillCountsThem() {
        val summary = QuoteReport.summarize(listOf(quoteOf("PLA", 20.0, 10.0, status = OrderStatus.ORCADO)))

        assertEquals(0, summary.quoteCount)
        assertEquals(1, summary.openQuoteCount)
        assertEquals(0.0, summary.conversionRate)
        assertNull(summary.mostUsedFilamentName)
        assertNull(summary.profitPerPrintHour)
    }

    @Test
    fun profitPerPrintHourUsesMachineTimeTimesQuantity() {
        // 2 peças de 30 min = 1 h de máquina; lucro 10 + 20 = 30 em 1 h + 0,5 h.
        val batch = quoteOf("PLA", salePrice = 20.0, productionCost = 10.0, printTimeMinutes = 30.0)
            .let { it.copy(quote = it.quote.copy(quantity = 2)) }
        val single = quoteOf("PLA", salePrice = 30.0, productionCost = 10.0, printTimeMinutes = 30.0)

        val summary = QuoteReport.summarize(listOf(batch, single))

        assertEquals(1.5, summary.printHours, 1e-9)
        assertEquals(20.0, summary.profitPerPrintHour!!, 1e-9)
    }

    @Test
    fun profitPerPrintHourIsNullWithoutPrintTime() {
        val summary = QuoteReport.summarize(listOf(quoteOf("PLA", 20.0, 10.0, printTimeMinutes = 0.0)))

        assertNull(summary.profitPerPrintHour)
    }

    @Test
    fun earningsPerLaborHourAddsLaborBackAndIgnoresOrdersWithoutLaborTime() {
        // 60 min de trabalho, R$ 25 de mão de obra dentro do custo, lucro 10: levou R$ 35 na hora.
        val withLabor = quoteOf("PLA", salePrice = 50.0, productionCost = 40.0).let {
            it.copy(quote = it.quote.copy(laborMinutes = 60.0, costs = it.quote.costs.copy(material = 15.0, labor = 25.0)))
        }
        val withoutLabor = quoteOf("PLA", salePrice = 100.0, productionCost = 10.0)

        val summary = QuoteReport.summarize(listOf(withLabor, withoutLabor))

        assertEquals(1.0, summary.laborHours, 1e-9)
        assertEquals(35.0, summary.earningsPerLaborHour!!, 1e-9)
    }

    @Test
    fun earningsPerLaborHourIsNullWhenNoOrderHasLaborTime() {
        assertNull(QuoteReport.summarize(listOf(quoteOf("PLA", 20.0, 10.0))).earningsPerLaborHour)
    }

    @Test
    fun productRankingGroupsSameNameIgnoringCaseAndSpaces() {
        val quotes = listOf(
            quoteOf("PLA", 20.0, 10.0, name = "Vaso"),
            quoteOf("PLA", 30.0, 10.0, name = " vaso ").copy(savedAtEpochMillis = 5L),
            quoteOf("PLA", 110.0, 70.0, name = "Chaveiro"),
            quoteOf("PLA", 90.0, 10.0, name = "Orçado", status = OrderStatus.ORCADO),
        )

        val ranking = QuoteReport.summarize(quotes).topProducts

        assertEquals(listOf("Chaveiro", "vaso"), ranking.map { it.name })
        assertEquals(2, ranking[1].orderCount)
        assertEquals(30.0, ranking[1].totalProfit)
        assertEquals(90.0, ranking[1].profitPerPrintHour!!, 1e-9)
    }

    @Test
    fun productRankingSkipsAutomaticNames() {
        val quotes = listOf(
            quoteOf("PLA", 20.0, 10.0, name = "Orçamento - 24/09/2026 14:30"),
            quoteOf("PLA", 20.0, 10.0, name = "Orçamento - suporte de fone"),
        )

        val ranking = QuoteReport.summarize(quotes).topProducts

        assertEquals(listOf("Orçamento - suporte de fone"), ranking.map { it.name })
    }

    @Test
    fun productRankingKeepsTopFive() {
        val quotes = (1..7).map { quoteOf("PLA", 10.0 + it, 10.0, name = "Peça $it") }

        val ranking = QuoteReport.summarize(quotes).topProducts

        assertEquals(QuoteSummary.RANKING_SIZE, ranking.size)
        assertEquals("Peça 7", ranking.first().name)
    }

    @Test
    fun discountRankingSumsNegotiatedDiscountPerClientAndDropsNetZeroOrBelow() {
        fun negotiated(client: String?, sale: Double, table: Double) = quoteOf("PLA", sale, 10.0).let {
            it.copy(client = client?.let(::Client), quote = it.quote.copy(tableSalePrice = table))
        }
        val quotes = listOf(
            negotiated("Ana", sale = 25.0, table = 30.0),
            negotiated("ana", sale = 27.0, table = 30.0),
            negotiated("Bruno", sale = 35.0, table = 30.0),
            negotiated(null, sale = 10.0, table = 30.0),
            quoteOf("PLA", 20.0, 10.0).copy(client = Client("Carla")),
        )

        val ranking = QuoteReport.summarize(quotes).topDiscountClients

        assertEquals(1, ranking.size)
        assertEquals(2, ranking[0].negotiatedCount)
        assertEquals(8.0, ranking[0].totalDiscount, 1e-9)
    }

    @Test
    fun catalogProductsStayOutOfSalesOpenQuotesAndConversion() {
        val quotes = listOf(
            quoteOf("PLA", salePrice = 20.0, productionCost = 10.0),
            quoteOf("PLA", salePrice = 30.0, productionCost = 10.0, status = OrderStatus.ORCADO),
            quoteOf("PETG", salePrice = 99.0, productionCost = 10.0, status = OrderStatus.ORCADO).copy(kind = QuoteKind.PRODUCT),
            quoteOf("PETG", salePrice = 98.0, productionCost = 10.0, status = OrderStatus.ENTREGUE).copy(kind = QuoteKind.PRODUCT),
        )

        val summary = QuoteReport.summarize(quotes)

        assertEquals(1, summary.quoteCount)
        assertEquals(20.0, summary.totalSalePrice)
        assertEquals(1, summary.openQuoteCount)
        assertEquals(30.0, summary.openQuoteTotal)
        assertEquals(0.5, summary.conversionRate)
        assertEquals("PLA", summary.mostUsedFilamentName)
    }

    @Test
    fun onlyCatalogProductsSummarizeToEmpty() {
        val summary = QuoteReport.summarize(listOf(quoteOf("PLA", 20.0, 10.0).copy(kind = QuoteKind.PRODUCT)))

        assertEquals(QuoteSummary.EMPTY, summary)
    }

    @Test
    fun productRankingJoinsSalesOfTheSameCatalogProductEvenAfterARename() {
        val quotes = listOf(
            quoteOf("PLA", 20.0, 10.0, name = "Chaveiro").copy(id = "a", sourceProductId = "p1", savedAtEpochMillis = 1L),
            quoteOf("PLA", 30.0, 10.0, name = "Chaveiro dragão").copy(id = "b", sourceProductId = "p1", savedAtEpochMillis = 2L),
            quoteOf("PLA", 25.0, 10.0, name = "chaveiro dragão ").copy(id = "c", savedAtEpochMillis = 3L), // sem origem, mesmo nome
            quoteOf("PLA", 50.0, 10.0, name = "Vaso").copy(id = "d"),
        )

        val ranking = QuoteReport.summarize(quotes).topProducts

        assertEquals(2, ranking.size)
        val keychain = ranking.first { it.orderCount == 3 }
        assertEquals("chaveiro dragão", keychain.name)
        assertEquals(45.0, keychain.totalProfit, 1e-9)
        assertEquals(1, ranking.first { it.name == "Vaso" }.orderCount)
    }

    @Test
    fun saleAtTheCatalogPriceIsNotANegotiationWithTheClient() {
        fun closed(sale: Double, table: Double, catalog: Boolean) = quoteOf("PLA", sale, 10.0).let {
            it.copy(
                id = "$sale-$catalog",
                client = Client(name = "Maria"),
                quote = it.quote.copy(tableSalePrice = table),
                soldAtCatalogPrice = catalog,
            )
        }
        val summary = QuoteReport.summarize(
            listOf(
                closed(sale = 18.0, table = 18.37, catalog = true),
                closed(sale = 18.9, table = 18.37, catalog = true),
                closed(sale = 15.0, table = 20.0, catalog = false),
            ),
        )

        assertEquals(1, summary.negotiatedCount)
        assertEquals(5.0, summary.totalNegotiatedDiscount, 1e-9)
        assertEquals(5.0, summary.topDiscountClients.single().totalDiscount, 1e-9)
    }

    @Test
    fun theSaleCountsInThePeriodTheClientClosedNotWhenTheQuoteWasCreated() {
        // Orçado em agosto (1.000), aprovado em setembro (5.000).
        val augustQuoteClosedInSeptember = quoteOf("PLA", 20.0, 10.0, status = OrderStatus.ORCADO)
            .copy(savedAtEpochMillis = 1_000L)
            .withStatus(OrderStatus.APROVADO, atEpochMillis = 5_000L)

        val august = QuoteReport.summarize(listOf(augustQuoteClosedInSeptember), periodStartEpochMillis = 0L, periodEndEpochMillis = 4_000L)
        val september = QuoteReport.summarize(listOf(augustQuoteClosedInSeptember), periodStartEpochMillis = 4_000L)

        assertEquals(0, august.quoteCount)
        assertEquals(1.0, august.conversionRate, "o orçamento criado em agosto já foi fechado")
        assertEquals(1, september.quoteCount)
        assertEquals(20.0, september.totalSalePrice)
    }

    @Test
    fun cancelledOrdersAreNeitherSalesNorOpenButLowerTheConversion() {
        val quotes = listOf(
            quoteOf("PLA", 20.0, 10.0, status = OrderStatus.APROVADO),
            quoteOf("PETG", 30.0, 10.0, status = OrderStatus.CANCELADO),
        )

        val summary = QuoteReport.summarize(quotes)

        assertEquals(1, summary.quoteCount)
        assertEquals(0, summary.openQuoteCount)
        assertEquals(0.5, summary.conversionRate)
    }

    @Test
    fun shippingIsNotCountedAsSales() {
        val withShipping = quoteOf("PLA", 20.0, 10.0).copy(shippingCost = 15.0)

        assertEquals(20.0, QuoteReport.summarize(listOf(withShipping)).totalSalePrice)
    }
}
