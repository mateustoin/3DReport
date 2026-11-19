package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.common_error_positive
import _3dreport.composeapp.generated.resources.common_error_required
import _3dreport.composeapp.generated.resources.stock_error_below_spool
import _3dreport.composeapp.generated.resources.stock_error_pick_item
import _3dreport.composeapp.generated.resources.stock_notice_link_failed
import _3dreport.composeapp.generated.resources.stock_notice_list_copied
import _3dreport.composeapp.generated.resources.stock_notice_purchase_saved
import _3dreport.composeapp.generated.resources.stock_notice_tracking
import com.threedreport.app.data.StockApp
import com.threedreport.app.data.StockSamples
import com.threedreport.app.data.StockSamples.saveOrder
import com.threedreport.app.data.TestClock
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

/** O ViewModel da tela Estoque: vistas, diálogos, painel do item, formulário de compra e lista de compras (Leva 12). */
class StockViewModelTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var dataDir: File
    private val red = StockItemRef.ColorRef("pla", "red")
    private val black = StockItemRef.ColorRef("pla", "black")
    private val ring = StockItemRef.ConsumableRef("ring")
    private val today = 20_000L

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
        // Cadastro vazio: sem os filamentos de exemplo da primeira abertura, só os do teste.
        File(dataDir, "filaments.json").writeText("[]")
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    private class Rig(val app: StockApp, val platform: FakePlatform, val viewModel: StockViewModel, val notices: List<UiNotice>) {
        val state get() = viewModel.uiState.value
        fun balance(ref: StockItemRef) = StockReport.balance(ref, app.stock.movements.value)
    }

    private fun rig(redInStock: Boolean = true): Rig {
        val app = StockApp(dataDir, TestClock(now = 1_000L))
        app.filaments.add(StockSamples.pla.copy(colors = listOf(StockSamples.red.copy(inStock = redInStock), StockSamples.black)))
        app.consumables.add(Consumable(id = "ring", name = "Argola", unitCost = 0.20))
        val platform = FakePlatform()
        var ids = 0
        val viewModel = StockViewModel(
            app.stock, app.filaments, app.consumables, app.history, scope,
            platform = platform, today = { today }, main = Dispatchers.Unconfined, newId = { "id-${++ids}" },
        )
        val notices = mutableListOf<UiNotice>()
        scope.launch { viewModel.notices.collect { notices += it } }
        return Rig(app, platform, viewModel, notices)
    }

    @Test
    fun theOverviewListsTrackedItemsByFilamentAndTheRestAsUntracked() {
        val rig = rig()
        rig.app.stock.startTracking(red, 640.0)
        rig.app.stock.startTracking(ring, 8.0)

        val state = rig.state
        assertEquals(1, state.filamentGroups.size)
        assertEquals("PLA Voolt", state.filamentGroups.single().filamentName)
        val row = state.filamentGroups.single().rows.single()
        assertEquals("PLA Voolt Vermelho", row.entry.displayName)
        assertEquals(640.0, row.status.balance, 1e-9)
        assertEquals(listOf(8.0), state.consumables.map { it.status.balance })
        assertEquals(listOf(black), state.untracked.map { it.ref })
        assertEquals(0, state.alertCount)
    }

    @Test
    fun anItemBelowItsLimitOrAtZeroCountsAsAnAlert() {
        val rig = rig()
        rig.app.stock.startTracking(red, 120.0)
        rig.app.stock.updateItemSettings(red, lowThreshold = 200.0, emptySpoolGrams = null)
        rig.app.stock.startTracking(ring, 0.0)

        assertEquals(2, rig.state.alertCount)
        assertEquals(StockState.LOW, rig.state.filamentGroups.single().rows.single().status.state)
        assertEquals(StockState.OUT, rig.state.consumables.single().status.state)
    }

    @Test
    fun trackingASpoolAsksForTheCurrentBalanceAndValidatesIt() {
        val rig = rig()
        val viewModel = rig.viewModel

        viewModel.startTrack(red)
        assertEquals(StockDialogKind.TRACK, rig.state.dialog?.kind)
        viewModel.confirmDialog()
        assertEquals(uiText(Res.string.common_error_required), rig.state.dialog?.error)
        viewModel.setDialogAmount("abc")
        viewModel.confirmDialog()
        assertEquals(uiText(Res.string.common_error_number_invalid), rig.state.dialog?.error)
        assertNull(rig.app.stock.itemFor(red))

        viewModel.setDialogAmount("1000")
        viewModel.confirmDialog()

        assertNull(rig.state.dialog)
        assertEquals(1000.0, rig.balance(red), 1e-9)
        assertEquals(listOf(uiText(Res.string.stock_notice_tracking)), rig.notices.map { it.message })
        assertTrue(rig.state.untracked.none { it.ref == red })
    }

    @Test
    fun aManualUseSubtractsWithItsReasonAndNote() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        val viewModel = rig.viewModel

        viewModel.startManualUse(red)
        viewModel.setDialogAmount("0")
        viewModel.confirmDialog()
        assertEquals(uiText(Res.string.common_error_positive), rig.state.dialog?.error)

        viewModel.setDialogAmount("60")
        viewModel.setDialogReason(ManualUseReason.TEST)
        viewModel.setDialogNote(" calibração ")
        viewModel.confirmDialog()

        assertNull(rig.state.dialog)
        assertEquals(940.0, rig.balance(red), 1e-9)
        val movement = rig.app.stock.movements.value.last()
        assertEquals(StockMovementKind.MANUAL_USE, movement.kind)
        assertEquals(ManualUseReason.TEST, movement.reason)
        assertEquals("calibração", movement.note)
    }

    @Test
    fun weighingShowsTheNewBalanceBeforeSavingAndRemembersTheEmptySpool() {
        val rig = rig()
        rig.app.stock.startTracking(red, 700.0)
        val viewModel = rig.viewModel

        viewModel.startWeigh(red)
        viewModel.setDialogAmount("1200")
        viewModel.setDialogSpool("250")

        val dialog = assertNotNull(rig.state.dialog)
        assertEquals(950.0, dialog.newBalance)
        assertEquals(700.0, dialog.currentBalance, 1e-9)

        viewModel.confirmDialog()

        assertNull(rig.state.dialog)
        assertEquals(950.0, rig.balance(red), 1e-9)
        assertEquals(250.0, rig.app.stock.itemFor(red)?.emptySpoolGrams)
        // Da próxima vez o carretel vazio já vem preenchido.
        viewModel.startWeigh(red)
        assertEquals("250", rig.state.dialog?.spoolText)
    }

    @Test
    fun weighingLessThanTheEmptySpoolIsAnError() {
        val rig = rig()
        rig.app.stock.startTracking(red, 700.0)

        rig.viewModel.startWeigh(red)
        rig.viewModel.setDialogAmount("100")
        rig.viewModel.setDialogSpool("250")
        rig.viewModel.confirmDialog()

        assertEquals(uiText(Res.string.stock_error_below_spool), rig.state.dialog?.error)
        assertEquals(700.0, rig.balance(red), 1e-9)
    }

    @Test
    fun adjustingTheBalanceWritesTheDifference() {
        val rig = rig()
        rig.app.stock.startTracking(ring, 8.0)

        rig.viewModel.startAdjust(ring)
        assertEquals("8", rig.state.dialog?.amountText)
        rig.viewModel.setDialogAmount("12,5")
        rig.viewModel.confirmDialog()

        assertEquals(12.5, rig.balance(ring), 1e-9)
        assertEquals(StockMovementKind.ADJUSTMENT, rig.app.stock.movements.value.last().kind)
    }

    @Test
    fun stoppingTheTrackingDeletesTheBalanceButKeepsThePurchases() {
        val rig = rig()
        rig.app.stock.startTracking(red, 500.0)
        rig.app.stock.savePurchase(Purchase("p1", today, lines = listOf(PurchaseLine(red, 1000.0, 90.0))))
        rig.viewModel.openItem(red)
        assertNotNull(rig.state.panel)

        rig.viewModel.requestStopTracking(red)
        assertEquals(StockDialogKind.STOP_TRACKING, rig.state.dialog?.kind)
        rig.viewModel.confirmDialog()

        assertNull(rig.app.stock.itemFor(red))
        assertTrue(rig.app.stock.movements.value.isEmpty())
        assertEquals(1, rig.app.stock.purchases.value.size)
        assertNull(rig.state.panel, "o painel do item que parou de ser controlado fecha")
        assertTrue(rig.state.untracked.any { it.ref == red })
    }

    @Test
    fun theItemPanelShowsTheNumbersAndSavesTheLimitAndTheSpool() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        rig.app.stock.savePurchase(Purchase("p1", today, link = "https://loja.example/pla", shipping = 10.0, lines = listOf(PurchaseLine(red, 1000.0, 90.0))))
        val order = rig.app.history.saveOrder(StockSamples.quote(grams = 180.0))
        rig.app.history.updateStatus(order.id, OrderStatus.APROVADO)

        rig.viewModel.openItem(red)
        val panel = (rig.state.panel as StockPanel.Item).panel

        assertEquals(2000.0, panel.status.balance, 1e-9)
        assertEquals(180.0, panel.status.reserved, 1e-9)
        assertEquals(1820.0, panel.status.available, 1e-9)
        assertEquals(100.0, panel.averageCost!!, 1e-9, "média das compras por kg, com o frete")
        assertEquals("https://loja.example/pla", panel.buyAgainLink)
        assertEquals(listOf(StockMovementKind.PURCHASE, StockMovementKind.START), panel.movements.map { it.kind })

        rig.viewModel.setThreshold("200")
        rig.viewModel.setSpool("250")
        rig.viewModel.saveItem()

        assertNull(rig.state.panel)
        assertEquals(200.0, rig.app.stock.itemFor(red)?.lowThreshold)
        assertEquals(250.0, rig.app.stock.itemFor(red)?.emptySpoolGrams)
    }

    @Test
    fun anInvalidLimitKeepsThePanelOpenWithTheError() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        rig.viewModel.openItem(red)

        rig.viewModel.setThreshold("muito")
        rig.viewModel.saveItem()

        assertEquals(uiText(Res.string.common_error_number_invalid), (rig.state.panel as StockPanel.Item).panel.thresholdError)
        assertNull(rig.app.stock.itemFor(red)?.lowThreshold)
    }

    @Test
    fun onlyManualUsesAndAdjustmentsCanBeDeleted() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        val use = rig.app.stock.recordManualUse(red, 100.0, ManualUseReason.FAILURE, null)!!
        rig.viewModel.openItem(red)
        val panel = (rig.state.panel as StockPanel.Item).panel
        assertEquals(listOf(true, false), panel.movements.map { it.removable })

        rig.viewModel.requestDeleteMovement(panel.movements.last().id)
        assertNull(rig.state.dialog, "o saldo inicial não se exclui")

        rig.viewModel.requestDeleteMovement(use.id)
        rig.viewModel.confirmDialog()
        assertEquals(1000.0, rig.balance(red), 1e-9)
    }

    // -------------------------------------------------------------------------------------------- compras

    @Test
    fun aPurchaseFormShowsTheCostPerKgWithTheShippingSharedAndTheCatalogUpdates() {
        val rig = rig()
        rig.app.stock.startTracking(red, 0.0)
        val viewModel = rig.viewModel

        viewModel.startPurchase()
        val form = (rig.state.panel as StockPanel.Purchase).panel.form
        assertEquals(today, form.dateEpochDay)
        assertEquals(1, form.lines.size)

        viewModel.updatePurchaseLine(1) { it.copy(ref = red, amountText = "1000", priceText = "89,90") }
        viewModel.updatePurchase { it.copy(shippingText = "7,50") }

        val panel = (rig.state.panel as StockPanel.Purchase).panel
        assertEquals(97.40, panel.lines.single().catalogUnitCost!!, 1e-9)
        assertEquals(97.40, panel.total, 1e-9)
        val update = panel.updates.single()
        assertEquals("PLA Voolt", update.name)
        assertEquals(89.90, update.currentValue, 1e-9)
        assertEquals(97.40, update.newValue, 1e-9)
        assertTrue(update.checked)
        assertTrue(panel.lines.single().tracked)
    }

    @Test
    fun savingAPurchaseAddsToTheBalanceAndUpdatesTheMarkedPrices() {
        val rig = rig()
        rig.app.stock.startTracking(red, 100.0)
        rig.app.stock.startTracking(ring, 10.0)
        val viewModel = rig.viewModel

        viewModel.startPurchase()
        viewModel.updatePurchaseLine(1) { it.copy(ref = red, amountText = "1000", priceText = "97,40") }
        viewModel.addPurchaseLine()
        viewModel.updatePurchaseLine(2) { it.copy(ref = ring, amountText = "50", priceText = "13") }
        viewModel.updatePurchase { it.copy(supplier = "  Voolt loja  ", link = " https://voolt.example ") }
        val panel = (rig.state.panel as StockPanel.Purchase).panel
        assertEquals(2, panel.updates.size)
        // Desmarca o do insumo: a compra grava sem mexer no custo dele.
        viewModel.toggleUpdate(panel.updates.last().key)
        assertFalse((rig.state.panel as StockPanel.Purchase).panel.updates.last().checked)

        viewModel.savePurchase()

        assertNull(rig.state.panel)
        assertEquals(listOf(uiText(Res.string.stock_notice_purchase_saved)), rig.notices.map { it.message })
        assertEquals(1100.0, rig.balance(red), 1e-9)
        assertEquals(60.0, rig.balance(ring), 1e-9)
        val saved = rig.app.stock.purchases.value.single()
        assertEquals("Voolt loja", saved.supplier)
        assertEquals("https://voolt.example", saved.link)
        assertEquals(today, saved.dateEpochDay)
        assertEquals(97.40, rig.app.filaments.filaments.value.single().pricePerKg, 1e-9)
        assertEquals(0.20, rig.app.consumables.consumables.value.single().unitCost, 1e-9, "caixa desmarcada: o cadastro não muda")
        assertEquals(1, rig.state.purchases.size)
        assertEquals(2, rig.state.purchases.single().lines.size)
    }

    @Test
    fun aPurchaseOfAnUntrackedColorMarksItAsInStockAndDoesNotAddGrams() {
        val rig = rig(redInStock = false)

        rig.viewModel.startPurchase()
        rig.viewModel.updatePurchaseLine(1) { it.copy(ref = red, amountText = "1000", priceText = "90") }
        assertFalse((rig.state.panel as StockPanel.Purchase).panel.lines.single().tracked)
        rig.viewModel.savePurchase()

        assertTrue(rig.app.stock.movements.value.isEmpty())
        assertTrue(rig.app.filaments.filaments.value.single().colors.first { it.id == "red" }.inStock)
        assertEquals(1, rig.app.stock.purchases.value.size)
    }

    @Test
    fun aPurchaseWithMissingFieldsStaysOpenWithTheErrorsOnTheLine() {
        val rig = rig()
        rig.viewModel.startPurchase()
        rig.viewModel.updatePurchaseLine(1) { it.copy(amountText = "0") }

        rig.viewModel.savePurchase()

        val line = (rig.state.panel as StockPanel.Purchase).panel.form.lines.single()
        assertEquals(uiText(Res.string.stock_error_pick_item), line.refError)
        assertEquals(uiText(Res.string.common_error_positive), line.amountError)
        assertEquals(uiText(Res.string.common_error_required), line.priceError)
        assertTrue(rig.app.stock.purchases.value.isEmpty())
    }

    @Test
    fun theQuickButtonsFillTheGrams() {
        val rig = rig()
        rig.viewModel.startPurchase()

        rig.viewModel.fillLineGrams(1, 500.0)
        assertEquals("500", (rig.state.panel as StockPanel.Purchase).panel.form.lines.single().amountText)
        rig.viewModel.fillLineGrams(1, 1000.0)
        assertEquals("1000", (rig.state.panel as StockPanel.Purchase).panel.form.lines.single().amountText)
    }

    @Test
    fun editingAPurchaseStartsWithThePriceUpdatesUnchecked() {
        val rig = rig()
        rig.app.stock.savePurchase(Purchase("p1", today - 3, supplier = "Voolt", lines = listOf(PurchaseLine(red, 1000.0, 97.40))))

        rig.viewModel.selectView(StockView.PURCHASES)
        rig.viewModel.openPurchase("p1")

        val panel = (rig.state.panel as StockPanel.Purchase).panel
        assertFalse(panel.isNew)
        assertEquals("Voolt", panel.form.supplier)
        assertEquals("97,40", panel.form.lines.single().priceText)
        assertFalse(panel.updates.single().checked, "reaplicar um preço antigo sem querer é pior que um clique a mais")
        assertTrue(rig.state.purchases.single().selected)

        rig.viewModel.savePurchase()
        assertEquals(89.90, rig.app.filaments.filaments.value.single().pricePerKg, 1e-9)
    }

    @Test
    fun deletingAPurchaseAsksFirstAndRemovesItsMovements() {
        val rig = rig()
        rig.app.stock.startTracking(red, 100.0)
        rig.app.stock.savePurchase(Purchase("p1", today, lines = listOf(PurchaseLine(red, 1000.0, 90.0))))
        assertEquals(1100.0, rig.balance(red), 1e-9)

        rig.viewModel.requestDeletePurchase("p1")
        assertEquals(StockDialogKind.DELETE_PURCHASE, rig.state.dialog?.kind)
        assertEquals(1, rig.app.stock.purchases.value.size)
        rig.viewModel.confirmDialog()

        assertTrue(rig.app.stock.purchases.value.isEmpty())
        assertEquals(100.0, rig.balance(red), 1e-9)
    }

    @Test
    fun purchasesAreListedNewestFirst() {
        val rig = rig()
        rig.app.stock.savePurchase(Purchase("old", today - 10, lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.app.stock.savePurchase(Purchase("new", today - 1, lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.app.stock.savePurchase(Purchase("mid", today - 5, lines = listOf(PurchaseLine(red, 500.0, 40.0))))

        assertEquals(listOf("new", "mid", "old"), rig.state.purchases.map { it.id })
    }

    // ----------------------------------------------------------------------------------- lista de compras

    private fun Rig.approvedOrderUsing(grams: Double) {
        val order = app.history.saveOrder(StockSamples.quote(grams = grams))
        app.history.updateStatus(order.id, OrderStatus.APROVADO)
    }

    @Test
    fun theShoppingListHasTheAmountToBuyAndTheCountOnTheTab() {
        val rig = rig()
        rig.app.stock.startTracking(red, 120.0)
        rig.app.stock.updateItemSettings(red, lowThreshold = 200.0, emptySpoolGrams = null)
        rig.approvedOrderUsing(180.0)

        val shopping = rig.state.shopping
        val row = shopping.lines.single()
        assertEquals(260.0, row.amount, 1e-9, "180 reservados + 200 de limite - 120 de saldo")
        assertEquals(180.0, row.line.reserved, 1e-9)
        assertEquals(1, shopping.size)
        assertTrue(shopping.hasTracked)
    }

    @Test
    fun copyingTheListPutsOneLinePerItemOnTheClipboard() {
        val rig = rig()
        rig.app.stock.startTracking(red, 120.0)
        rig.app.stock.updateItemSettings(red, lowThreshold = 200.0, emptySpoolGrams = null)
        rig.approvedOrderUsing(180.0)

        rig.viewModel.copyShoppingList()
        // O texto da unidade vem de recurso (lido de outra thread na primeira vez): espera a lista chegar.
        val deadline = System.currentTimeMillis() + 10_000
        while (rig.platform.copied.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10)

        assertEquals(listOf("PLA Voolt Vermelho: 260 g"), rig.platform.copied)
        assertEquals(listOf(uiText(Res.string.stock_notice_list_copied)), rig.notices.map { it.message })
    }

    @Test
    fun registeringAPurchaseFromTheListFillsTheLinesWithoutPrices() {
        val rig = rig()
        rig.app.stock.startTracking(red, 120.0)
        rig.app.stock.updateItemSettings(red, lowThreshold = 200.0, emptySpoolGrams = null)
        rig.approvedOrderUsing(180.0)

        rig.viewModel.selectView(StockView.SHOPPING)
        rig.viewModel.startPurchaseFromShopping()

        val line = (rig.state.panel as StockPanel.Purchase).panel.form.lines.single()
        assertEquals(red, line.ref)
        assertEquals("260", line.amountText)
        assertEquals("", line.priceText)
    }

    @Test
    fun anUntrackedColorMarkedOutOfStockAndUsedByAnOrderGoesToTheSeparateSection() {
        val rig = rig()
        // Vermelho sem controle e marcado "Acabou", usado por um pedido aprovado.
        val pla = rig.app.filaments.filaments.value.single()
        rig.app.filaments.update(pla.copy(colors = pla.colors.map { if (it.id == "red") it.copy(inStock = false) else it }))
        rig.approvedOrderUsing(100.0)

        val shopping = rig.state.shopping
        assertTrue(shopping.lines.isEmpty())
        assertEquals(red, shopping.outOfStock.single().entry.ref)
        assertEquals(1, shopping.outOfStock.single().orderCount)
        assertFalse(shopping.hasTracked)
    }

    @Test
    fun buyingAgainOpensTheLinkOfTheLastPurchaseThatHadOne() {
        val rig = rig()
        rig.app.stock.savePurchase(Purchase("p1", today - 5, link = "https://old.example", lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.app.stock.savePurchase(Purchase("p2", today - 1, link = "https://new.example", lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.app.stock.savePurchase(Purchase("p3", today, lines = listOf(PurchaseLine(red, 500.0, 40.0))))

        rig.viewModel.buyAgain(red)

        assertEquals(listOf("https://new.example"), rig.platform.opened)
    }

    @Test
    fun aLinkThatDoesNotOpenGivesAnErrorNotice() {
        val rig = rig()
        rig.app.stock.savePurchase(Purchase("p1", today, link = "https://x.example", lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.platform.browserWorks = false

        rig.viewModel.buyAgain(red)

        val notice = rig.notices.single()
        assertEquals(uiText(Res.string.stock_notice_link_failed), notice.message)
        assertTrue(notice.isError)
    }

    @Test
    fun theViewAndThePanelAreMutuallyExclusive() {
        val rig = rig()
        rig.app.stock.startTracking(red, 100.0)

        rig.viewModel.selectView(StockView.PURCHASES)
        assertEquals(StockView.PURCHASES, rig.state.view)
        rig.viewModel.openItem(red)
        assertTrue(rig.state.panel is StockPanel.Item)
        rig.viewModel.startPurchase()
        assertTrue(rig.state.panel is StockPanel.Purchase)
        rig.viewModel.closePanel()
        assertNull(rig.state.panel)
    }

    @Test
    fun noItemsInTheRegistersIsEmpty() {
        val app = StockApp(dataDir, TestClock())
        val viewModel = StockViewModel(app.stock, app.filaments, app.consumables, app.history, scope, platform = FakePlatform(), today = { today })

        assertTrue(viewModel.uiState.value.hasNoItems)
        assertTrue(viewModel.uiState.value.shopping.isEmpty)
    }

    // ------------------------------------------------------------------- desempenho e rascunho

    @Test
    fun typingInTheFormsDoesNotRecomputeTheStockFromTheRepositories() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        val before = rig.viewModel.dataBuilds

        rig.viewModel.startPurchase()
        rig.viewModel.updatePurchaseLine(1) { it.copy(ref = red) }
        "1000".forEach { c -> rig.viewModel.updatePurchaseLine(1) { it.copy(amountText = it.amountText + c) } }
        "97,40".forEach { c -> rig.viewModel.updatePurchaseLine(1) { it.copy(priceText = it.priceText + c) } }
        rig.viewModel.updatePurchase { it.copy(supplier = "Voolt") }
        rig.viewModel.openItem(red)
        rig.viewModel.setThreshold("200")
        rig.viewModel.startManualUse(red)
        rig.viewModel.setDialogAmount("12")
        rig.viewModel.selectView(StockView.SHOPPING)

        assertEquals(before, rig.viewModel.dataBuilds)
        // O que depende do rascunho acompanha: o painel e o diálogo seguem o que foi digitado.
        assertEquals("12", rig.state.dialog?.amountText)

        // Mudou um repositório: refaz uma vez.
        rig.app.stock.recordManualUse(red, 10.0, ManualUseReason.TEST, null)
        assertEquals(before + 1, rig.viewModel.dataBuilds)
    }

    private fun Rig.halfFilledPurchase() {
        viewModel.startPurchase()
        viewModel.updatePurchase { it.copy(supplier = "Voolt") }
    }

    @Test
    fun switchingPanelWithAHalfFilledPurchaseAsksBeforeDiscarding() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        rig.halfFilledPurchase()

        rig.viewModel.openItem(red)

        assertEquals(StockDialogKind.DISCARD_PURCHASE, rig.state.dialog?.kind)
        assertTrue(rig.state.panel is StockPanel.Purchase, "a compra continua aberta até a resposta")
        assertEquals("Voolt", (rig.state.panel as StockPanel.Purchase).panel.form.supplier)

        rig.viewModel.dismissDialog()
        assertNull(rig.state.dialog)
        assertEquals("Voolt", (rig.state.panel as StockPanel.Purchase).panel.form.supplier, "Voltar deixa a compra como estava")

        rig.viewModel.openItem(red)
        rig.viewModel.confirmDialog()
        assertNull(rig.state.dialog)
        assertTrue(rig.state.panel is StockPanel.Item, "Descartar faz a troca que foi pedida")
    }

    @Test
    fun everyWayOfReplacingTheOpenPurchaseAsksFirst() {
        val rig = rig()
        rig.app.stock.savePurchase(Purchase("p1", today, lines = listOf(PurchaseLine(red, 500.0, 40.0))))
        rig.app.stock.startTracking(red, 100.0)
        rig.approvedOrderUsing(500.0)

        for (replace in listOf<() -> Unit>(rig.viewModel::startPurchase, rig.viewModel::startPurchaseFromShopping, { rig.viewModel.openPurchase("p1") })) {
            rig.halfFilledPurchase()
            replace()
            assertEquals(StockDialogKind.DISCARD_PURCHASE, rig.state.dialog?.kind)
            rig.viewModel.dismissDialog()
            rig.viewModel.closePanel()
        }
    }

    @Test
    fun anUntouchedPurchaseFormIsReplacedWithoutAsking() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        rig.viewModel.startPurchase()

        rig.viewModel.openItem(red)

        assertNull(rig.state.dialog)
        assertTrue(rig.state.panel is StockPanel.Item)
    }

    @Test
    fun cancellingOrSavingAPurchaseNeverAsks() {
        val rig = rig()
        rig.app.stock.startTracking(red, 1000.0)
        rig.halfFilledPurchase()
        rig.viewModel.closePanel()
        assertNull(rig.state.panel)

        rig.viewModel.startPurchase()
        rig.viewModel.updatePurchaseLine(1) { it.copy(ref = red, amountText = "500", priceText = "40") }
        rig.viewModel.savePurchase()
        rig.viewModel.openItem(red)

        assertNull(rig.state.dialog)
        assertTrue(rig.state.panel is StockPanel.Item)
    }
}
