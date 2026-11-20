package com.threedreport.app.data

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.QuotedPrint
import java.io.File

/** Relógio dos testes do estoque: anda só quando o teste manda. */
internal class TestClock(var now: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = now
}

/**
 * O app montado como no [com.threedreport.app.createDesktopContainer], com estoque ligado aos pedidos e aos filamentos,
 * sobre a pasta [dataDir]. Cada instância lê do disco: criar outra é como abrir o app de novo.
 */
internal class StockApp(dataDir: File, val clock: TestClock = TestClock()) {
    private val storage = LocalStorage(dataDir, clock)
    val stock: StockRepository = storage.stock()
    val filaments: FilamentRepository = storage.filaments().deletingStockWith(stock)
    val consumables: ConsumableRepository = storage.consumables()
    val history: QuoteHistoryRepository = storage.quoteHistory().reconcilingStock(stock) { filaments.filaments.value }
}

internal object StockSamples {
    val red = FilamentColor(id = "red", name = "Vermelho", hex = "#E53935")
    val black = FilamentColor(id = "black", name = "Preto", hex = "#000000")
    val pla = Filament(id = "pla", name = "PLA Voolt", pricePerKg = 89.90, densityGPerCm3 = 1.24, colors = listOf(red, black))

    /** Um orçamento que gasta [grams] g de PLA na [color] e, se [rings] > 0, essa quantidade de argolas por peça. */
    fun quote(grams: Double = 100.0, color: FilamentColor = red, rings: Double = 0.0, quantity: Int = 1): Quote {
        val job = PrintJob(filaments = listOf(FilamentUsage(pla, pla.lengthMeters(grams), color)), printTimeMinutes = 60.0)
        return Quote(
            prints = listOf(QuotedPrint(job = job, printerId = "p", printerName = "Impressora", cost = PrintCost(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))),
            costs = CostBreakdown(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            salePrice = 20.0,
            quantity = quantity,
            consumables = if (rings > 0) listOf(QuotedConsumable(id = "ring", name = "Argola", unitCost = 0.2, quantity = rings, chargedPerOrder = false)) else emptyList(),
        )
    }

    fun QuoteHistoryRepository.saveOrder(quote: Quote = quote()) =
        save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null)
}
