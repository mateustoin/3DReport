package com.threedreport.app.data

import com.threedreport.core.model.CatalogPriceUpdate
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.StockReport
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O estoque grava e lê o saldo, os movimentos e as compras (Leva 12), e limpa o controle da cor que sai do filamento. */
class StockRepositoryTest {

    private lateinit var dataDir: File
    private val red = StockItemRef.ColorRef("pla", "red")
    private val black = StockItemRef.ColorRef("pla", "black")
    private val ring = StockItemRef.ConsumableRef("ring")
    private val argola = Consumable(id = "ring", name = "Argola", unitCost = 0.20)

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun app(): StockApp = StockApp(dataDir).also {
        if (it.filaments.filaments.value.none { f -> f.id == "pla" }) it.filaments.add(StockSamples.pla)
        if (it.consumables.consumables.value.isEmpty()) it.consumables.add(argola)
    }

    private fun StockApp.balance(ref: StockItemRef) = StockReport.balance(ref, stock.movements.value)

    @Test
    fun startingTrackingRecordsTheBalanceAndSurvivesReopening() {
        val app = app()

        val item = app.stock.startTracking(red, 1000.0)

        assertEquals(red, item.ref)
        assertEquals(app.clock.now, item.trackedSinceEpochMillis)
        assertEquals(StockMovementKind.START, app.stock.movements.value.single().kind)
        val reopened = StockApp(dataDir)
        assertEquals(item, reopened.stock.itemFor(red))
        assertEquals(1000.0, reopened.balance(red), 1e-9)
        assertTrue(File(dataDir, "stock-items.json").exists())
        assertTrue(File(dataDir, "stock-movements.json").exists())
    }

    @Test
    fun startingTwiceKeepsTheFirstControl() {
        val app = app()
        val first = app.stock.startTracking(red, 1000.0)

        assertEquals(first, app.stock.startTracking(red, 50.0))
        assertEquals(1000.0, app.balance(red), 1e-9)
    }

    @Test
    fun manualUseAdjustAndWeighingChangeTheBalance() {
        val app = app()
        app.stock.startTracking(red, 1000.0)

        assertNotNull(app.stock.recordManualUse(red, 40.0, ManualUseReason.FAILURE, "espaguete"))
        assertEquals(960.0, app.balance(red), 1e-9)

        assertNotNull(app.stock.adjustTo(red, 700.0))
        assertEquals(700.0, app.balance(red), 1e-9)
        assertNull(app.stock.adjustTo(red, 700.0), "saldo igual: nada a gravar")

        assertNotNull(app.stock.recordWeighing(red, scaleGrams = 890.0, emptySpoolGrams = 250.0))
        assertEquals(640.0, app.balance(red), 1e-9)
        assertEquals(250.0, StockApp(dataDir).stock.itemFor(red)?.emptySpoolGrams, "o carretel vazio fica lembrado")
    }

    @Test
    fun nothingIsRecordedForAnItemWithoutControl() {
        val app = app()

        assertNull(app.stock.recordManualUse(red, 40.0, ManualUseReason.TEST, null))
        assertNull(app.stock.adjustTo(red, 10.0))
        assertTrue(app.stock.movements.value.isEmpty())
    }

    @Test
    fun onlyManualAndAdjustmentMovementsCanBeDeletedByHand() {
        val app = app()
        app.stock.startTracking(red, 1000.0)
        val use = app.stock.recordManualUse(red, 40.0, ManualUseReason.OWN_USE, null)!!
        val start = app.stock.movements.value.first()

        assertFalse(app.stock.deleteMovement(start.id))
        assertTrue(app.stock.deleteMovement(use.id))
        assertEquals(1000.0, app.balance(red), 1e-9)
    }

    @Test
    fun limitAndEmptySpoolAreSavedOnTheItem() {
        val app = app()
        app.stock.startTracking(red, 1000.0)

        app.stock.updateItemSettings(red, lowThreshold = 200.0, emptySpoolGrams = 240.0)

        val item = StockApp(dataDir).stock.itemFor(red)!!
        assertEquals(200.0, item.lowThreshold)
        assertEquals(240.0, item.emptySpoolGrams)
    }

    @Test
    fun stoppingDeletesTheControlAndMovementsButKeepsPurchases() {
        val app = app()
        app.stock.startTracking(red, 1000.0)
        app.stock.savePurchase(Purchase(id = "p", dateEpochDay = 1, lines = listOf(PurchaseLine(red, 1000.0, 90.0))))

        app.stock.stopTracking(red)

        val reopened = StockApp(dataDir)
        assertNull(reopened.stock.itemFor(red))
        assertTrue(reopened.stock.movements.value.isEmpty())
        assertEquals(1, reopened.stock.purchases.value.size)
    }

    @Test
    fun aPurchaseAddsToTrackedItemsAndEditingAndDeletingRedoIt() {
        val app = app()
        app.stock.startTracking(red, 100.0)
        val purchase = Purchase(id = "p", dateEpochDay = 1, supplier = "  Voolt ", shipping = 10.0, lines = listOf(PurchaseLine(red, 1000.0, 90.0), PurchaseLine(black, 500.0, 50.0)))

        val saved = app.stock.savePurchase(purchase)
        assertEquals("Voolt", saved.supplier)
        assertEquals(1100.0, app.balance(red), 1e-9)
        assertEquals(0.0, app.balance(black), 1e-9, "cor sem controle: a compra não soma gramas")

        app.stock.savePurchase(saved.copy(lines = listOf(PurchaseLine(red, 500.0, 45.0))))
        assertEquals(600.0, app.balance(red), 1e-9)
        assertEquals(1, app.stock.purchases.value.size)

        app.stock.deletePurchase("p")
        assertEquals(100.0, StockApp(dataDir).balance(red), 1e-9)
        assertTrue(StockApp(dataDir).stock.purchases.value.isEmpty())
    }

    @Test
    fun supplierKeepsTheSpellingAlreadyUsed() {
        val app = app()
        app.stock.savePurchase(Purchase(id = "a", dateEpochDay = 1, supplier = "Voolt", lines = listOf(PurchaseLine(red, 1.0, 1.0))))

        val second = app.stock.savePurchase(Purchase(id = "b", dateEpochDay = 2, supplier = "voolt", lines = listOf(PurchaseLine(red, 1.0, 1.0))))

        assertEquals("Voolt", second.supplier)
        assertEquals(listOf("Voolt"), app.stock.supplierSuggestions())
    }

    @Test
    fun recordingAPurchaseUpdatesOnlyTheCheckedPrices() {
        val app = app()
        val purchase = Purchase(id = "p", dateEpochDay = 1, shipping = 18.0, lines = listOf(PurchaseLine(red, 1000.0, 89.90), PurchaseLine(ring, 50.0, 12.0)))
        val updates = purchase.catalogPriceUpdates(app.filaments.filaments.value, app.consumables.consumables.value)
        val filamentUpdate = updates.filterIsInstance<CatalogPriceUpdate.FilamentPrice>().single()

        app.stock.recordPurchase(purchase, listOf(filamentUpdate), app.filaments, app.consumables)

        val reopened = StockApp(dataDir)
        assertEquals(filamentUpdate.newPricePerKg, reopened.filaments.filaments.value.single { it.id == "pla" }.pricePerKg, 1e-9)
        assertEquals(0.20, reopened.consumables.consumables.value.single().unitCost, "desmarcado: o cadastro não muda")
    }

    @Test
    fun buyingAColorWithoutControlMarksItInStock() {
        val app = app()
        val pla = app.filaments.filaments.value.single { it.id == "pla" }
        app.filaments.update(pla.copy(colors = pla.colors.map { it.copy(inStock = false) }))

        app.stock.recordPurchase(Purchase(id = "p", dateEpochDay = 1, lines = listOf(PurchaseLine(red, 1000.0, 90.0))), emptyList(), app.filaments, app.consumables)

        val colors = StockApp(dataDir).filaments.filaments.value.single { it.id == "pla" }.colors.associate { it.id to it.inStock }
        assertEquals(mapOf("red" to true, "black" to false), colors)
        assertEquals(89.90, StockApp(dataDir).filaments.filaments.value.single { it.id == "pla" }.pricePerKg, "sem caixa marcada, o preço fica")
    }

    @Test
    fun removingAColorFromTheFilamentDeletesItsControl() {
        val app = app()
        app.stock.startTracking(red, 1000.0)
        app.stock.startTracking(black, 1000.0)
        app.stock.recordManualUse(black, 40.0, ManualUseReason.TEST, null)

        // A cor não tem lixeira: tirar a preta do filamento apaga só o controle e os movimentos dela.
        val pla = app.filaments.filaments.value.single { it.id == "pla" }
        app.filaments.update(pla.copy(colors = pla.colors.filter { it.id != "black" }))

        val reopened = StockApp(dataDir)
        assertNull(reopened.stock.itemFor(black))
        assertTrue(reopened.stock.movements.value.none { it.ref == black })
        assertNotNull(reopened.stock.itemFor(red))
        assertEquals(1000.0, reopened.balance(red), 1e-9)
    }

    @Test
    fun deletingAFilamentOrAConsumableKeepsTheControlForWhenItIsRestored() {
        val app = app()
        // Os dois abaixo do limite: entram no alerta e na lista de compras enquanto existem.
        app.stock.startTracking(red, 100.0)
        app.stock.updateItemSettings(red, lowThreshold = 500.0, emptySpoolGrams = null)
        app.stock.recordManualUse(red, 30.0, ManualUseReason.FAILURE, "espaguete")
        app.stock.startTracking(ring, 2.0)
        app.stock.updateItemSettings(ring, lowThreshold = 10.0, emptySpoolGrams = null)
        val items = app.stock.items.value
        val movements = app.stock.movements.value
        assertEquals(2, app.overview().alertCount)
        assertEquals(setOf(red, ring), app.shoppingList().lines.map { it.ref }.toSet())

        app.filaments.delete("pla")
        app.consumables.delete("ring")

        val deleted = StockApp(dataDir)
        assertEquals(items, deleted.stock.items.value, "o controle fica")
        assertEquals(movements, deleted.stock.movements.value, "os movimentos ficam")
        val overview = deleted.overview()
        assertTrue(overview.tracked.isEmpty(), "excluído não aparece no estoque")
        assertTrue(overview.untracked.none { it == red || it == ring }, "nem como item sem controle")
        assertEquals(0, overview.alertCount, "nem no alerta")
        assertTrue(deleted.shoppingList().isEmpty, "nem na lista de compras")

        deleted.filaments.restore("pla")
        deleted.consumables.restore("ring")

        val restored = StockApp(dataDir)
        assertEquals(70.0, restored.balance(red), 1e-9)
        assertEquals(2.0, restored.balance(ring), 1e-9)
        assertEquals(movements, restored.stock.movements.value)
        assertEquals(2, restored.overview().alertCount)
        assertEquals(setOf(red, ring), restored.shoppingList().lines.map { it.ref }.toSet())
    }

    private fun StockApp.overview() =
        StockReport.overview(stock.items.value, stock.movements.value, history.savedQuotes.value, filaments.filaments.value, consumables.consumables.value)

    private fun StockApp.shoppingList() =
        StockReport.shoppingList(stock.items.value, stock.movements.value, history.savedQuotes.value, filaments.filaments.value, consumables.consumables.value)

    @Test
    fun archivingKeepsTheControl() {
        val app = app()
        app.stock.startTracking(red, 1000.0)

        app.filaments.setArchived("pla", true)

        assertNotNull(app.stock.itemFor(red))
    }
}
