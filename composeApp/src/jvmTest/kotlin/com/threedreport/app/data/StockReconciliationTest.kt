package com.threedreport.app.data

import com.threedreport.app.data.StockSamples.saveOrder
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.StockReport
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A baixa dos pedidos ligada ao histórico (item 4 da Leva 12): o roteiro do teste manual do responsável, com os
 * repositórios de verdade gravando na pasta do teste.
 */
class StockReconciliationTest {

    private lateinit var dataDir: File
    private val red = StockItemRef.ColorRef("pla", "red")

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun StockApp.balance() = StockReport.balance(red, stock.movements.value)

    private fun StockApp.reserved() = StockReport.reserved(red, history.savedQuotes.value, filaments.filaments.value)

    private fun appWithRedTracked(): StockApp {
        val app = StockApp(dataDir)
        app.filaments.add(StockSamples.pla)
        app.stock.startTracking(red, 1000.0)
        app.clock.now += 1_000
        return app
    }

    @Test
    fun readyDiscountsBackToApprovedReturnsAndDeletingKeeps() {
        val app = appWithRedTracked()
        val order = app.history.saveOrder(StockSamples.quote(grams = 100.0))

        app.history.updateStatus(order.id, OrderStatus.APROVADO)
        assertEquals(1000.0, app.balance(), 1e-6)
        assertEquals(100.0, app.reserved(), 1e-6)

        app.history.updateStatus(order.id, OrderStatus.PRONTO)
        assertEquals(900.0, app.balance(), 1e-6)
        val movement = app.stock.movements.value.single { it.kind == StockMovementKind.ORDER }
        assertEquals(order.id, movement.quoteId)
        assertEquals(order.number, movement.orderNumber)

        app.history.updateStatus(order.id, OrderStatus.APROVADO)
        assertEquals(1000.0, app.balance(), 1e-6)
        assertEquals(100.0, app.reserved(), 1e-6)

        app.history.updateStatus(order.id, OrderStatus.PRONTO)
        app.history.delete(order.id)
        assertEquals(900.0, app.balance(), 1e-6, "pedido excluído: o material foi gasto")

        app.history.restore(order.id)
        assertEquals(900.0, app.balance(), 1e-6, "restaurar não desconta de novo")

        // Tudo gravado: abrir o app de novo dá o mesmo saldo.
        assertEquals(900.0, StockApp(dataDir).balance(), 1e-6)
    }

    @Test
    fun cancellingAReadyOrderGivesTheMaterialBack() {
        val app = appWithRedTracked()
        val order = app.history.saveOrder()
        app.history.updateStatus(order.id, OrderStatus.PRONTO)

        app.history.updateStatus(order.id, OrderStatus.CANCELADO)

        assertEquals(1000.0, app.balance(), 1e-6)
    }

    @Test
    fun editingAReadyOrderRedoesTheDiscount() {
        val app = appWithRedTracked()
        val order = app.history.saveOrder(StockSamples.quote(grams = 100.0))
        app.history.updateStatus(order.id, OrderStatus.PRONTO)

        app.history.update(order.id, order.name, StockSamples.quote(grams = 250.0), emptyList(), null, null, null, null)

        assertEquals(750.0, app.balance(), 1e-6)
        assertEquals(1, app.stock.movements.value.count { it.kind == StockMovementKind.ORDER })
    }

    @Test
    fun openingTheAppWithOrdersAlreadyReadyDiscountsNothing() {
        // Uma versão sem estoque: pedido já Pronto, sem controle.
        val before = StockApp(dataDir)
        before.filaments.add(StockSamples.pla)
        val order = before.history.saveOrder()
        before.history.updateStatus(order.id, OrderStatus.PRONTO)

        // Abre de novo, liga o controle e mexe no pedido sem tirar de Pronto.
        val app = StockApp(dataDir, TestClock(now = 50_000L))
        app.stock.startTracking(red, 1000.0)
        app.clock.now += 1_000
        app.history.updateStatus(order.id, OrderStatus.ENTREGUE)
        app.history.update(order.id, order.name, StockSamples.quote(grams = 300.0), emptyList(), null, null, null, null)
        app.history.delete(order.id)
        app.history.restore(order.id)

        assertEquals(1000.0, app.balance(), 1e-6)
        assertTrue(app.stock.movements.value.none { it.kind == StockMovementKind.ORDER })
    }

    @Test
    fun theFirstListWhenTheAppOpensIsOnlyTheBase() {
        val app = appWithRedTracked()
        val order = app.history.saveOrder()
        app.history.updateStatus(order.id, OrderStatus.PRONTO)
        val movements = app.stock.movements.value

        // Abrir o app de novo (com o pedido Pronto e o movimento gravado) não cria nem tira nada.
        val reopened = StockApp(dataDir, TestClock(now = 90_000L))
        reopened.history.recordFollowUp(order.id)

        assertEquals(movements, reopened.stock.movements.value)
    }

    @Test
    fun untrackedColorsAreNotDiscounted() {
        val app = appWithRedTracked()
        val order = app.history.saveOrder(StockSamples.quote(color = StockSamples.black))

        app.history.updateStatus(order.id, OrderStatus.PRONTO)

        assertTrue(app.stock.movements.value.none { it.kind == StockMovementKind.ORDER })
    }

    @Test
    fun consumablesAreDiscountedInUnits() {
        val app = appWithRedTracked()
        val ring = StockItemRef.ConsumableRef("ring")
        app.stock.startTracking(ring, 100.0)
        app.clock.now += 1_000
        val order = app.history.saveOrder(StockSamples.quote(rings = 2.0, quantity = 5))

        app.history.updateStatus(order.id, OrderStatus.PRONTO)

        assertEquals(90.0, StockReport.balance(ring, app.stock.movements.value), 1e-9)
    }
}
