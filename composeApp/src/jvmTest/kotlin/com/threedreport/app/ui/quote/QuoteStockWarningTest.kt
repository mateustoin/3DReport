package com.threedreport.app.ui.quote

import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.StockApp
import com.threedreport.app.data.StockSamples
import com.threedreport.app.data.StockSamples.saveOrder
import com.threedreport.app.data.TestClock
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.StockItemRef
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** O aviso em âmbar do Orçamento quando o pedido usa mais do que sobra livre (item 13 da Leva 12). Só avisa, só pra pedido. */
class QuoteStockWarningTest {

    private lateinit var dataDir: File
    private val red = StockItemRef.ColorRef("pla", "red")
    private val ring = StockItemRef.ConsumableRef("ring")

    @BeforeTest
    fun setUp() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
        // Cadastro vazio: sem os filamentos de exemplo da primeira abertura, só os do teste.
        File(dataDir, "filaments.json").writeText("[]")
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun app(): StockApp = StockApp(dataDir, TestClock()).also {
        it.filaments.add(StockSamples.pla)
        it.consumables.add(Consumable(id = "ring", name = "Argola", unitCost = 0.20))
    }

    private fun viewModel(app: StockApp, withStock: Boolean = true) = QuoteViewModel(
        app.filaments, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), app.history,
        consumableRepository = app.consumables,
        stockRepository = app.stock.takeIf { withStock },
    ).apply {
        selectFilament("pla")
        selectFilamentColor("red")
        setWeightGrams("320")
        setPrintTimeMinutes("190")
    }

    private val QuoteViewModel.warnings get() = uiState.value.stockWarnings

    @Test
    fun anOrderThatUsesMoreThanTheFreeBalanceGetsAWarningWithTheNumbers() {
        val app = app()
        app.stock.startTracking(red, 430.0)
        val reserved = app.history.saveOrder(StockSamples.quote(grams = 180.0))
        app.history.updateStatus(reserved.id, OrderStatus.APROVADO)

        val vm = viewModel(app)
        val warning = vm.warnings.single()

        assertEquals(red, warning.ref)
        assertEquals("PLA Voolt Vermelho", warning.name)
        assertEquals(320.0, warning.needed, 1.0)
        assertEquals(250.0, warning.available, 1e-6)
        assertEquals(180.0, warning.reserved, 1e-6)
        assertEquals(setOf(vm.uiState.value.input.prints.single().id), warning.printIds, "a impressão que usa a cor")
    }

    @Test
    fun noWarningWhenTheBalanceCoversItAndNoneForAnUntrackedColor() {
        val app = app()
        val covered = viewModel(app)
        assertTrue(covered.warnings.isEmpty(), "cor sem controle não avisa nada")

        app.stock.startTracking(red, 5000.0)
        assertTrue(covered.warnings.isEmpty())
    }

    @Test
    fun theWarningFollowsTheBalanceWithoutReopeningTheScreen() {
        val app = app()
        app.stock.startTracking(red, 1000.0)
        val vm = viewModel(app)
        assertTrue(vm.warnings.isEmpty())

        app.stock.recordManualUse(red, 800.0, com.threedreport.core.model.ManualUseReason.FAILURE, null)

        assertEquals(1, vm.warnings.size)
    }

    @Test
    fun aProductFromTheCatalogNeverWarns() {
        val app = app()
        app.stock.startTracking(red, 10.0)
        val vm = viewModel(app)
        assertEquals(1, vm.warnings.size)

        vm.setKind(QuoteKind.PRODUCT)

        assertTrue(vm.warnings.isEmpty())
    }

    @Test
    fun withoutTheStockRepositoryThereIsNeverAWarning() {
        val app = app()
        app.stock.startTracking(red, 10.0)

        assertTrue(viewModel(app, withStock = false).warnings.isEmpty())
    }

    @Test
    fun aConsumableThatFallsShortGetsItsOwnWarning() {
        val app = app()
        app.stock.startTracking(ring, 3.0)
        val vm = viewModel(app)
        vm.toggleConsumable("ring")
        vm.setConsumableQuantity("ring", "5")

        val warning = vm.warnings.single()

        assertEquals(ring, warning.ref)
        assertEquals("Argola", warning.name)
        assertEquals(5.0, warning.needed, 1e-9)
        assertEquals(3.0, warning.available, 1e-9)
        assertTrue(warning.printIds.isEmpty())
    }

    @Test
    fun editingAnApprovedOrderDoesNotCountItTwice() {
        val app = app()
        app.stock.startTracking(red, 400.0)
        val order = app.history.saveOrder(StockSamples.quote(grams = 320.0))
        app.history.updateStatus(order.id, OrderStatus.APROVADO)

        // Reaberto, o pedido de 320 g cabe nos 400 g: ele mesmo sai do reservado.
        val vm = viewModel(app)
        vm.loadForEditing(app.history.savedQuotes.value.single { it.id == order.id })
        assertTrue(vm.warnings.isEmpty(), vm.warnings.toString())

        // Um pedido novo igual, sim, falta: os 320 g do primeiro já estão reservados.
        assertEquals(1, viewModel(app).warnings.size)
    }

    @Test
    fun editingAReadyOrderDoesNotWarnBecauseItsMaterialAlreadyLeftTheBalance() {
        val app = app()
        app.stock.startTracking(red, 400.0)
        val order = app.history.saveOrder(StockSamples.quote(grams = 320.0))
        app.history.updateStatus(order.id, OrderStatus.PRONTO)
        assertNotNull(app.stock.movements.value.firstOrNull { it.quoteId == order.id })

        val vm = viewModel(app)
        vm.loadForEditing(app.history.savedQuotes.value.single { it.id == order.id })

        assertTrue(vm.warnings.isEmpty())
    }

    @Test
    fun typingInTheQuoteDoesNotRecomputeTheReservedOfAllOrders() {
        val app = app()
        app.stock.startTracking(red, 430.0)
        val reserved = app.history.saveOrder(StockSamples.quote(grams = 180.0))
        app.history.updateStatus(reserved.id, OrderStatus.APROVADO)
        val vm = viewModel(app)
        assertEquals(1, vm.warnings.size)
        val before = vm.stockSnapshotBuilds

        // Cada tecla refaz a conta do orçamento, mas o reservado de todos os pedidos continua o guardado.
        listOf("321", "322", "323", "324").forEach { vm.setWeightGrams(it) }
        vm.setPrintTimeMinutes("200")

        assertEquals(before, vm.stockSnapshotBuilds)
        assertEquals(1, vm.warnings.size)

        // Mudou o estoque: refaz uma vez.
        app.stock.recordManualUse(red, 10.0, com.threedreport.core.model.ManualUseReason.TEST, null)
        vm.setWeightGrams("325")
        assertEquals(before + 1, vm.stockSnapshotBuilds)
    }
}
