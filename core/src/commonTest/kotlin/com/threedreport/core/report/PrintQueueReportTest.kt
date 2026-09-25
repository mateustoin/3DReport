package com.threedreport.core.report

import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals

class PrintQueueReportTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private fun printerOf(id: String, name: String) = PrinterProfile(
        id = id,
        name = name,
        printerPowerWatts = 200.0,
        maintenanceCostPerHour = 0.1,
        machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
    )

    private fun quoteOf(id: String, printerId: String?, status: OrderStatus, printTimeMinutes: Double) = SavedQuote(
        id = id,
        name = "Peça $id",
        quote = Quote(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = printTimeMinutes),
            filamentWeightGrams = 5.0,
            costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            productionCost = 0.0,
            salePrice = 0.0,
            printerId = printerId,
        ),
        savedAtEpochMillis = 0L,
        status = status,
    )

    @Test
    fun sumsPrintTimeOnlyForQuotesCurrentlyPrintingOnThatPrinter() {
        val printerA = printerOf("a", "Impressora A")
        val printerB = printerOf("b", "Impressora B")
        val quotes = listOf(
            quoteOf("1", printerId = "a", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 120.0),
            quoteOf("2", printerId = "a", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 60.0),
            quoteOf("3", printerId = "a", status = OrderStatus.ENTREGUE, printTimeMinutes = 999.0), // não conta: não está em impressão
            quoteOf("4", printerId = "b", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 30.0),
        )

        val queue = PrintQueueReport.summarize(listOf(printerA, printerB), quotes)

        val entryA = queue.first { it.printer.id == "a" }
        val entryB = queue.first { it.printer.id == "b" }
        assertEquals(180.0, entryA.queuedMinutes)
        assertEquals(2, entryA.queuedQuoteCount)
        assertEquals(30.0, entryB.queuedMinutes)
        assertEquals(1, entryB.queuedQuoteCount)
    }

    @Test
    fun approvedQuotesCountWhenAskedForTheDeliveryHint() {
        // A dica de prazo considera também o que foi aprovado e ainda espera a vez de imprimir.
        val printer = printerOf("a", "Impressora A")
        val quotes = listOf(
            quoteOf("1", printerId = "a", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 120.0),
            quoteOf("2", printerId = "a", status = OrderStatus.APROVADO, printTimeMinutes = 60.0),
            quoteOf("3", printerId = "a", status = OrderStatus.ORCADO, printTimeMinutes = 999.0),
        )

        val nowPrinting = PrintQueueReport.summarize(listOf(printer), quotes).single()
        val ahead = PrintQueueReport.summarize(
            listOf(printer),
            quotes,
            statuses = setOf(OrderStatus.APROVADO, OrderStatus.EM_IMPRESSAO),
        ).single()

        assertEquals(120.0, nowPrinting.queuedMinutes)
        assertEquals(180.0, ahead.queuedMinutes)
        assertEquals(2, ahead.queuedQuoteCount)
    }

    @Test
    fun printerWithNothingQueuedHasZeroMinutes() {
        val printer = printerOf("a", "Impressora A")

        val queue = PrintQueueReport.summarize(listOf(printer), emptyList())

        assertEquals(0.0, queue.single().queuedMinutes)
        assertEquals(0, queue.single().queuedQuoteCount)
    }

    @Test
    fun quotesWithoutAPrinterIdAreIgnored() {
        // Orçamentos salvos antes de Quote.printerId existir (decisão 64) não entram em nenhuma fila.
        val printer = printerOf("a", "Impressora A")
        val quotes = listOf(quoteOf("1", printerId = null, status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 500.0))

        val queue = PrintQueueReport.summarize(listOf(printer), quotes)

        assertEquals(0.0, queue.single().queuedMinutes)
    }

    @Test
    fun catalogProductsNeverEnterTheQueue() {
        val printer = printerOf("a", "Impressora A")
        val quotes = listOf(
            quoteOf("1", printerId = "a", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 60.0),
            quoteOf("2", printerId = "a", status = OrderStatus.EM_IMPRESSAO, printTimeMinutes = 500.0).copy(kind = QuoteKind.PRODUCT),
        )

        val entry = PrintQueueReport.summarize(listOf(printer), quotes).single()

        assertEquals(60.0, entry.queuedMinutes)
        assertEquals(1, entry.queuedQuoteCount)
    }
}
