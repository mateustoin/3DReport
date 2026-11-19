package com.threedreport.app.ui.consumables

import com.threedreport.app.data.StockApp
import com.threedreport.app.data.TestClock
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.StockItemRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Insumos com o estoque da Leva 12: a linha do insumo controlado mostra o saldo ("8 un em estoque"). */
class ConsumableListViewModelStockTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var dataDir: File
    private val ring = StockItemRef.ConsumableRef("ring")

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun aTrackedConsumableShowsItsBalanceAndFollowsChanges() {
        val app = StockApp(dataDir, TestClock())
        app.consumables.add(Consumable(id = "ring", name = "Argola", unitCost = 0.20))
        app.consumables.add(Consumable(id = "box", name = "Caixa", unitCost = 2.0))
        val viewModel = ConsumableListViewModel(app.consumables, scope, stock = app.stock)
        assertTrue(viewModel.uiState.value.balances.isEmpty(), "sem controle, nada muda na linha")

        app.stock.startTracking(ring, 8.0)
        assertEquals(mapOf("ring" to 8.0), viewModel.uiState.value.balances.mapValues { it.value.amount })

        app.stock.recordManualUse(ring, 2.5, com.threedreport.core.model.ManualUseReason.TEST, null)
        assertEquals(5.5, viewModel.uiState.value.balances.getValue("ring").amount, 1e-9)

        app.stock.stopTracking(ring)
        assertTrue(viewModel.uiState.value.balances.isEmpty())
    }

    @Test
    fun withoutTheStockRepositoryThereAreNoBalances() {
        val app = StockApp(dataDir, TestClock())
        app.consumables.add(Consumable(id = "ring", name = "Argola", unitCost = 0.20))
        app.stock.startTracking(ring, 8.0)

        assertTrue(ConsumableListViewModel(app.consumables, scope).uiState.value.balances.isEmpty())
    }
}
