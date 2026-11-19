package com.threedreport.core.report

import com.threedreport.core.model.Consumable
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.StockMovement
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.StockFixtures.abs
import com.threedreport.core.report.StockFixtures.absWhite
import com.threedreport.core.report.StockFixtures.black
import com.threedreport.core.report.StockFixtures.box
import com.threedreport.core.report.StockFixtures.filaments
import com.threedreport.core.report.StockFixtures.item
import com.threedreport.core.report.StockFixtures.order
import com.threedreport.core.report.StockFixtures.petg
import com.threedreport.core.report.StockFixtures.petgDefault
import com.threedreport.core.report.StockFixtures.pla
import com.threedreport.core.report.StockFixtures.plaBlack
import com.threedreport.core.report.StockFixtures.plaRed
import com.threedreport.core.report.StockFixtures.quote
import com.threedreport.core.report.StockFixtures.red
import com.threedreport.core.report.StockFixtures.ring
import com.threedreport.core.report.StockFixtures.usage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StockReportTest {

    private val ringConsumable = Consumable(id = "ring", name = "Argola", unitCost = 0.2)
    private val boxConsumable = Consumable(id = "box", name = "Caixa", unitCost = 1.5, chargedPerOrder = true)
    private val consumables = listOf(boxConsumable, ringConsumable)

    private fun movement(ref: com.threedreport.core.model.StockItemRef, amount: Double, id: String = "m-$ref-$amount") =
        StockMovement(id = id, ref = ref, epochMillis = 0L, amount = amount, kind = StockMovementKind.ADJUSTMENT)

    // --- Consumo por pedido (item 5) ---

    @Test
    fun filamentConsumptionMultipliesRunsAndQuantityAndSumsByColor() {
        val q = quote(usages = listOf(usage(pla, 100.0, red), usage(pla, 20.0, black), usage(pla, 5.0, red)), runs = 2, quantity = 3)

        val consumption = StockReport.consumptionOf(q, filaments)

        assertEquals(105.0 * 2 * 3, consumption.getValue(plaRed), 1e-6)
        assertEquals(20.0 * 2 * 3, consumption.getValue(plaBlack), 1e-6)
    }

    @Test
    fun nullColorGoesToTheOnlyColorOfTheFilament() {
        val consumption = StockReport.consumptionOf(quote(usages = listOf(usage(petg, 50.0, null))), filaments)

        assertEquals(50.0, consumption.getValue(petgDefault), 1e-6)
    }

    @Test
    fun nullColorAndTheOnlyColorWrittenOutSumInTheSameItem() {
        val q = quote(usages = listOf(usage(petg, 50.0, null), usage(petg, 30.0, petg.colors.single())), runs = 2, quantity = 3)

        val consumption = StockReport.consumptionOf(q, filaments)

        assertEquals(listOf(petgDefault), consumption.keys.toList())
        assertEquals(80.0 * 2 * 3, consumption.getValue(petgDefault), 1e-6)
    }

    @Test
    fun nullColorOfAFilamentWithManyColorsIsNotAttributed() {
        val consumption = StockReport.consumptionOf(quote(usages = listOf(usage(pla, 50.0, null))), filaments)

        assertTrue(consumption.isEmpty())
    }

    @Test
    fun consumptionUsesTheSnapshotOfTheOrderAndNotTheCatalog() {
        val q = quote(usages = listOf(usage(pla, 100.0, red)))
        // O cadastro mudou a densidade depois: a baixa continua a do pedido.
        val changed = listOf(pla.copy(densityGPerCm3 = 2.0))

        assertEquals(100.0, StockReport.consumptionOf(q, changed).getValue(plaRed), 1e-6)
    }

    @Test
    fun consumablesCountPerPieceOrPerOrder() {
        val q = quote(
            quantity = 4,
            consumables = listOf(
                QuotedConsumable(id = "ring", name = "Argola", unitCost = 0.2, quantity = 2.0, chargedPerOrder = false),
                QuotedConsumable(id = "box", name = "Caixa", unitCost = 1.5, quantity = 1.0, chargedPerOrder = true),
            ),
        )

        val consumption = StockReport.consumptionOf(q, filaments)

        assertEquals(8.0, consumption.getValue(ring), 1e-9)
        assertEquals(1.0, consumption.getValue(box), 1e-9)
    }

    // --- Saldo, reservado, disponível e estado (itens 3, 10 e 11) ---

    @Test
    fun balanceIsTheSumOfTheMovementsOfTheItem() {
        val movements = listOf(movement(plaRed, 1000.0), movement(plaRed, -120.0), movement(plaBlack, 500.0))

        assertEquals(880.0, StockReport.balance(plaRed, movements), 1e-9)
        assertEquals(0.0, StockReport.balance(ring, movements), 1e-9)
    }

    @Test
    fun reservedCountsOnlyApprovedAndPrintingOrders() {
        val quotes = listOf(
            order("a", OrderStatus.APROVADO),
            order("b", OrderStatus.EM_IMPRESSAO),
            order("c", OrderStatus.ORCADO),
            order("d", OrderStatus.PRONTO),
            order("e", OrderStatus.CANCELADO),
            order("f", OrderStatus.APROVADO, kind = QuoteKind.PRODUCT),
        )

        assertEquals(200.0, StockReport.reservedByRef(quotes, filaments).getValue(plaRed), 1e-6)
        assertEquals(100.0, StockReport.reservedByRef(quotes, filaments, excludingQuoteId = "a").getValue(plaRed), 1e-6)
    }

    @Test
    fun stateIsOutAtZeroOrLessAndLowBelowTheThreshold() {
        assertEquals(StockState.OUT, StockReport.state(item(plaRed), 0.0))
        assertEquals(StockState.OUT, StockReport.state(item(plaRed, lowThreshold = 200.0), -40.0))
        assertEquals(StockState.LOW, StockReport.state(item(plaRed, lowThreshold = 200.0), 120.0))
        assertEquals(StockState.OK, StockReport.state(item(plaRed, lowThreshold = 200.0), 200.0))
        assertEquals(StockState.OK, StockReport.state(item(plaRed), 1.0))
    }

    @Test
    fun aBalanceThatIsZeroOnlyByRoundingIsOut() {
        val movements = listOf(movement(plaRed, 0.1, "a"), movement(plaRed, 0.2, "b"), movement(plaRed, -0.3, "c"))

        assertEquals(StockState.OUT, StockReport.state(item(plaRed), StockReport.balance(plaRed, movements)))
    }

    // --- Aviso do Orçamento (item 13) ---

    @Test
    fun shortageWarnsWhenTheQuoteUsesMoreThanIsFree() {
        val items = listOf(item(plaRed), item(plaBlack))
        val movements = listOf(movement(plaRed, 430.0), movement(plaBlack, 1000.0))
        val quotes = listOf(order("approved", OrderStatus.APROVADO, quote(usages = listOf(usage(pla, 180.0, red)))))
        val draft = quote(usages = listOf(usage(pla, 320.0, red), usage(pla, 10.0, black)))

        val shortages = StockReport.shortages(draft, items, movements, quotes, filaments)

        val shortage = shortages.single()
        assertEquals(plaRed, shortage.ref)
        assertEquals(320.0, shortage.needed, 1e-6)
        assertEquals(250.0, shortage.available, 1e-6)
        assertEquals(180.0, shortage.reserved, 1e-6)
    }

    @Test
    fun editingAReservedOrderTakesItOutOfTheReserved() {
        val items = listOf(item(plaRed))
        val movements = listOf(movement(plaRed, 300.0))
        val editing = order("approved", OrderStatus.APROVADO, quote(usages = listOf(usage(pla, 250.0, red))))

        val shortages = StockReport.shortages(editing.quote, items, movements, listOf(editing), filaments, excludingQuoteId = "approved")

        assertTrue(shortages.isEmpty())
    }

    @Test
    fun untrackedColorsNeverWarn() {
        val shortages = StockReport.shortages(quote(usages = listOf(usage(pla, 900.0, red))), emptyList(), emptyList(), emptyList(), filaments)

        assertTrue(shortages.isEmpty())
    }

    // --- Lista de compras (item D) ---

    @Test
    fun shoppingListBuysReservedPlusThresholdMinusBalance() {
        val items = listOf(item(plaRed, lowThreshold = 200.0), item(plaBlack), item(ring, lowThreshold = 20.0))
        val movements = listOf(movement(plaRed, 120.0), movement(plaBlack, 1000.0), movement(ring, 8.0))
        val quotes = listOf(order("a", OrderStatus.APROVADO, quote(usages = listOf(usage(pla, 180.0, red)))))

        val list = StockReport.shoppingList(items, movements, quotes, filaments, consumables)

        assertEquals(listOf(plaRed, ring), list.lines.map { it.ref })
        val red = list.lines.first()
        assertEquals(260.0, red.amount, 1e-6)
        assertEquals(180.0, red.reserved, 1e-6)
        assertEquals(120.0, red.balance, 1e-6)
        assertEquals(200.0, red.lowThreshold)
        assertEquals(12.0, list.lines[1].amount, 1e-9)
    }

    @Test
    fun shoppingListOrdersFilamentsByNameThenColorThenConsumables() {
        val items = listOf(item(ring, lowThreshold = 1.0), item(plaRed, lowThreshold = 1.0), item(petgDefault, lowThreshold = 1.0), item(plaBlack, lowThreshold = 1.0))

        val list = StockReport.shoppingList(items, emptyList(), emptyList(), filaments, consumables)

        // PETG 3D Fila < PLA Voolt; Preto < Vermelho; insumos no fim.
        assertEquals(listOf(petgDefault, plaBlack, plaRed, ring), list.lines.map { it.ref })
    }

    @Test
    fun untrackedColorsMarkedOutAndUsedByReservedOrdersGoToTheirOwnSection() {
        val absQuote = quote(usages = listOf(usage(abs, 50.0, StockFixtures.white)))
        val quotes = listOf(
            order("a", OrderStatus.APROVADO, absQuote),
            order("b", OrderStatus.EM_IMPRESSAO, absQuote),
            order("c", OrderStatus.ORCADO, absQuote),
        )

        val list = StockReport.shoppingList(emptyList(), emptyList(), quotes, filaments, consumables)

        assertTrue(list.lines.isEmpty())
        val out = list.outOfStockColors.single()
        assertEquals(absWhite, out.ref)
        assertEquals(2, out.orderCount)
    }

    @Test
    fun archivedAndDeletedItemsAreLeftOutOfTheShoppingList() {
        val items = listOf(item(plaRed, lowThreshold = 100.0), item(StockItemRefs.ghost, lowThreshold = 100.0))

        val list = StockReport.shoppingList(items, emptyList(), emptyList(), listOf(pla.copy(archived = true), petg, abs), consumables)

        assertTrue(list.lines.isEmpty())
    }

    @Test
    fun nothingToBuyGivesAnEmptyList() {
        val list = StockReport.shoppingList(listOf(item(plaRed)), listOf(movement(plaRed, 100.0)), emptyList(), filaments, consumables)

        assertTrue(list.isEmpty)
    }

    // --- Visão geral (itens 12 e 27) ---

    @Test
    fun overviewSplitsTrackedFromUntrackedAndCountsAlerts() {
        val items = listOf(item(plaRed, lowThreshold = 200.0), item(petgDefault), item(ring))
        val movements = listOf(movement(plaRed, 120.0), movement(petgDefault, 1000.0))

        val overview = StockReport.overview(items, movements, emptyList(), filaments, consumables)

        assertEquals(listOf(petgDefault, plaRed, ring), overview.tracked.map { it.item.ref })
        assertEquals(listOf(absWhite, plaBlack, box), overview.untracked)
        // PLA vermelho abaixo do limite e a argola com saldo zero.
        assertEquals(2, overview.alertCount)
        assertEquals(StockState.LOW, overview.tracked[1].state)
    }

    @Test
    fun overviewHidesArchivedFilamentsAndConsumables() {
        val overview = StockReport.overview(
            listOf(item(plaRed)),
            emptyList(),
            emptyList(),
            listOf(pla.copy(archived = true)),
            listOf(ringConsumable.copy(archived = true)),
        )

        assertTrue(overview.tracked.isEmpty())
        assertTrue(overview.untracked.isEmpty())
        assertEquals(0, overview.alertCount)
    }

    // --- Média das compras e "Comprar de novo" (itens 18 e 19) ---

    @Test
    fun averagePurchaseCostIsWeightedByAmountInTheCatalogUnit() {
        val purchases = listOf(
            Purchase(id = "p1", dateEpochDay = 1, lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0))),
            Purchase(id = "p2", dateEpochDay = 2, shipping = 10.0, lines = listOf(PurchaseLine(plaRed, 500.0, 50.0))),
            Purchase(id = "p3", dateEpochDay = 3, lines = listOf(PurchaseLine(ring, 50.0, 10.0))),
        )

        // (90 + 60) / 1,5 kg = R$ 100/kg
        assertEquals(100.0, StockReport.averagePurchaseCost(plaRed, purchases)!!, 1e-9)
        assertEquals(0.2, StockReport.averagePurchaseCost(ring, purchases)!!, 1e-9)
        assertNull(StockReport.averagePurchaseCost(plaBlack, purchases))
    }

    @Test
    fun buyAgainUsesTheLinkOfTheLatestPurchaseThatHadOne() {
        val purchases = listOf(
            Purchase(id = "p1", dateEpochDay = 1, link = "https://a", lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0))),
            Purchase(id = "p2", dateEpochDay = 3, link = "https://b", lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0))),
            Purchase(id = "p3", dateEpochDay = 5, lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0))),
            Purchase(id = "p4", dateEpochDay = 9, link = "https://c", lines = listOf(PurchaseLine(ring, 1.0, 1.0))),
        )

        assertEquals("https://b", StockReport.lastPurchaseLink(plaRed, purchases))
        assertNull(StockReport.lastPurchaseLink(plaBlack, purchases))
    }

    @Test
    fun supplierSuggestionsKeepTheFirstSpellingAndIgnoreCase() {
        val purchases = listOf(
            Purchase(id = "p1", dateEpochDay = 1, supplier = "Voolt", lines = listOf(PurchaseLine(plaRed, 1.0, 1.0))),
            Purchase(id = "p2", dateEpochDay = 2, supplier = "voolt", lines = listOf(PurchaseLine(plaRed, 1.0, 1.0))),
            Purchase(id = "p3", dateEpochDay = 3, supplier = "3D Fila", lines = listOf(PurchaseLine(plaRed, 1.0, 1.0))),
        )

        assertEquals(listOf("3D Fila", "Voolt"), StockReport.suppliers(purchases))
        assertEquals("Voolt", StockReport.normalizedSupplier("  VOOLT ", purchases))
        assertEquals("Nova loja", StockReport.normalizedSupplier(" Nova loja ", purchases))
        assertNull(StockReport.normalizedSupplier("   ", purchases))
    }
}

private object StockItemRefs {
    val ghost = com.threedreport.core.model.StockItemRef.ColorRef("apagado", "x")
}
