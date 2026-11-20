package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PurchaseTest {

    private val red = StockItemRef.ColorRef("pla", "red")
    private val black = StockItemRef.ColorRef("pla", "black")
    private val ring = StockItemRef.ConsumableRef("ring")

    private val pla = Filament(
        id = "pla",
        name = "PLA Voolt",
        pricePerKg = 89.90,
        densityGPerCm3 = 1.24,
        colors = listOf(FilamentColor("red"), FilamentColor("black")),
    )
    private val argola = Consumable(id = "ring", name = "Argola", unitCost = 0.20)

    /** O exemplo do esboço da spec: PLA 1 kg a R$ 89,90 e 50 argolas a R$ 12,00, com R$ 18,00 de frete. */
    private val purchase = Purchase(
        id = "p",
        dateEpochDay = 20_775,
        shipping = 18.0,
        lines = listOf(PurchaseLine(red, 1000.0, 89.90), PurchaseLine(ring, 50.0, 12.0)),
    )

    @Test
    fun shippingIsSharedByTheValueOfEachLine() {
        val total = 89.90 + 12.0
        assertEquals(18.0 * 89.90 / total, purchase.shippingShare(purchase.lines[0]), 1e-9)
        assertEquals(18.0 * 12.0 / total, purchase.shippingShare(purchase.lines[1]), 1e-9)
        assertEquals(18.0, purchase.lines.sumOf { purchase.shippingShare(it) }, 1e-9)
    }

    @Test
    fun lineCostIsPricePlusShippingShareAndUnitCostDividesByAmount() {
        val line = purchase.lines[0]
        val cost = 89.90 + 18.0 * 89.90 / 101.90
        assertEquals(cost, purchase.lineCost(line), 1e-9)
        assertEquals(cost / 1000.0, purchase.unitCost(line), 1e-12)
        // R$ 105,78/kg: o que o formulário mostra ao lado da linha.
        assertEquals(cost, purchase.catalogUnitCost(line), 1e-9)
        assertEquals((12.0 + 18.0 * 12.0 / 101.90) / 50.0, purchase.catalogUnitCost(purchase.lines[1]), 1e-12)
    }

    @Test
    fun totalIsLinesPlusShipping() {
        assertEquals(89.90 + 12.0 + 18.0, purchase.total, 1e-9)
    }

    @Test
    fun shippingIsSplitEquallyWhenEveryLineIsFree() {
        val free = Purchase(id = "p", dateEpochDay = 0, shipping = 10.0, lines = listOf(PurchaseLine(red, 100.0, 0.0), PurchaseLine(ring, 5.0, 0.0)))

        assertEquals(5.0, free.shippingShare(free.lines[0]), 1e-9)
        assertEquals(5.0, free.shippingShare(free.lines[1]), 1e-9)
    }

    @Test
    fun twoColorsOfTheSameFilamentMakeOneCostPerKg() {
        val two = Purchase(id = "p", dateEpochDay = 0, lines = listOf(PurchaseLine(red, 1000.0, 90.0), PurchaseLine(black, 500.0, 60.0)))

        assertEquals(mapOf("pla" to 100.0), two.costPerKgByFilament())
    }

    @Test
    fun unitCostByConsumableSumsItsLines() {
        val two = Purchase(id = "p", dateEpochDay = 0, lines = listOf(PurchaseLine(ring, 50.0, 10.0), PurchaseLine(ring, 50.0, 14.0)))

        assertEquals(0.24, two.unitCostByConsumable().getValue("ring"), 1e-12)
    }

    @Test
    fun priceUpdatesListOnlyWhatChangesMoreThanHalfACent() {
        val updates = purchase.catalogPriceUpdates(listOf(pla), listOf(argola))

        val filament = updates.filterIsInstance<CatalogPriceUpdate.FilamentPrice>().single()
        assertEquals("pla", filament.filamentId)
        assertEquals(89.90, filament.currentPricePerKg)
        // 89,90 + 15,88 de frete = 105,7801...: vai pro cadastro em centavos.
        assertEquals(105.78, filament.newPricePerKg, 1e-9)
        val consumable = updates.filterIsInstance<CatalogPriceUpdate.ConsumableCost>().single()
        assertEquals("ring", consumable.consumableId)
        assertEquals(0.20, consumable.currentUnitCost)
        // (12 + 2,1197...) / 50 = 0,28239...: 4 casas no insumo.
        assertEquals(0.2824, consumable.newUnitCost, 1e-9)

        // Pagou o mesmo preço do cadastro: nada a perguntar.
        val same = Purchase(id = "s", dateEpochDay = 0, lines = listOf(PurchaseLine(red, 1000.0, 89.904)))
        assertTrue(same.catalogPriceUpdates(listOf(pla), emptyList()).isEmpty())
    }

    @Test
    fun priceUpdateOfADeletedItemIsSkipped() {
        assertTrue(purchase.catalogPriceUpdates(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun applyingAnUpdateChangesOnlyThePrice() {
        val update = CatalogPriceUpdate.FilamentPrice("pla", 89.90, 97.40)

        assertEquals(pla.copy(pricePerKg = 97.40), update.applyTo(pla))
        assertEquals(argola.copy(unitCost = 0.26), CatalogPriceUpdate.ConsumableCost("ring", 0.20, 0.26).applyTo(argola))
    }

    @Test
    fun aPurchaseNeedsLinesWithPositiveAmountAndNonNegativePrice() {
        assertFailsWith<IllegalArgumentException> { Purchase(id = "p", dateEpochDay = 0, lines = emptyList()) }
        assertFailsWith<IllegalArgumentException> { PurchaseLine(red, 0.0, 1.0) }
        assertFailsWith<IllegalArgumentException> { PurchaseLine(red, 10.0, -1.0) }
        assertFailsWith<IllegalArgumentException> { Purchase(id = "p", dateEpochDay = 0, shipping = -1.0, lines = listOf(PurchaseLine(red, 1.0, 1.0))) }
    }

    @Test
    fun theRefsOfAPurchaseAreListedOnce() {
        val two = Purchase(id = "p", dateEpochDay = 0, lines = listOf(PurchaseLine(red, 1.0, 1.0), PurchaseLine(red, 2.0, 1.0), PurchaseLine(ring, 1.0, 1.0)))

        assertEquals(mapOf(red to 3.0, ring to 1.0), two.amountByRef())
    }
}
