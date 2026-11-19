package com.threedreport.core.report

import com.threedreport.core.costsOf
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StatusChange
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.quotedPrint

/** Cadastro e pedidos de teste do estoque (Leva 12). */
internal object StockFixtures {
    val red = FilamentColor(id = "red", name = "Vermelho", hex = "#E53935")
    val black = FilamentColor(id = "black", name = "Preto", hex = "#000000")
    val white = FilamentColor(id = "white", name = "Branco", hex = "#FFFFFF", inStock = false)

    /** PLA com duas cores. */
    val pla = Filament(id = "pla", name = "PLA Voolt", pricePerKg = 89.90, densityGPerCm3 = 1.24, colors = listOf(red, black))

    /** PETG de uma cor só (sem nome), pra cor `null` cair nela. */
    val petg = Filament(id = "petg", name = "PETG 3D Fila", pricePerKg = 100.0, densityGPerCm3 = 1.27, colors = listOf(FilamentColor(id = "default")))

    /** ABS com uma cor marcada "Acabou". */
    val abs = Filament(id = "abs", name = "ABS", pricePerKg = 80.0, densityGPerCm3 = 1.04, colors = listOf(white))

    val filaments = listOf(pla, petg, abs)

    val plaRed = StockItemRef.ColorRef("pla", "red")
    val plaBlack = StockItemRef.ColorRef("pla", "black")
    val petgDefault = StockItemRef.ColorRef("petg", "default")
    val absWhite = StockItemRef.ColorRef("abs", "white")
    val ring = StockItemRef.ConsumableRef("ring")
    val box = StockItemRef.ConsumableRef("box")

    /** Um uso de [grams] gramas (por rodada) de [filament] em [color]. */
    fun usage(filament: Filament, grams: Double, color: FilamentColor? = null) =
        FilamentUsage(filament, filament.lengthMeters(grams), color)

    fun quote(
        usages: List<FilamentUsage> = listOf(usage(pla, 100.0, red)),
        runs: Int = 1,
        quantity: Int = 1,
        consumables: List<QuotedConsumable> = emptyList(),
    ) = Quote(
        prints = listOf(quotedPrint(PrintJob(filaments = usages, printTimeMinutes = 60.0, runs = runs))),
        costs = costsOf(),
        salePrice = 10.0,
        quantity = quantity,
        consumables = consumables,
    )

    fun order(
        id: String,
        status: OrderStatus,
        quote: Quote = quote(),
        kind: QuoteKind = QuoteKind.ORDER,
        number: Int = 1,
        history: List<StatusChange> = listOf(StatusChange(OrderStatus.ORCADO, 0L)) + (if (status != OrderStatus.ORCADO) listOf(StatusChange(status, 0L)) else emptyList()),
        savedAt: Long = 0L,
    ) = SavedQuote(
        id = id,
        name = "Pedido $id",
        quote = quote,
        savedAtEpochMillis = savedAt,
        status = status,
        kind = kind,
        number = number,
        statusHistory = history,
    )

    fun item(ref: StockItemRef, since: Long = 0L, lowThreshold: Double? = null) =
        StockItem(id = "item-$ref", ref = ref, trackedSinceEpochMillis = since, lowThreshold = lowThreshold)
}
