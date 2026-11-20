package com.threedreport.app.data

import com.threedreport.app.data.StockSamples.saveOrder
import com.threedreport.core.model.CatalogPriceUpdate
import com.threedreport.core.model.FilamentImportPlan
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PaymentMethod
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O roteiro do teste manual da Leva 12 inteiro, numa história só e na ordem em que o responsável testou: cada passo
 * parte do saldo que o anterior deixou, com os repositórios de verdade gravando na pasta do teste. Os casos isolados
 * estão em [StockReconciliationTest] e [StockRepositoryTest]; aqui o que se confere é que eles combinam.
 */
class StockManualScriptTest {

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

    private fun StockApp.reserved() = StockReport.reservedByRef(history.savedQuotes.value, filaments.filaments.value)[red] ?: 0.0

    private fun StockApp.shoppingList() = StockReport.shoppingList(
        stock.items.value,
        stock.movements.value,
        history.savedQuotes.value,
        filaments.filaments.value,
        consumables.consumables.value,
    )

    private fun StockApp.pla() = filaments.filaments.value.single { it.id == StockSamples.pla.id }

    private fun StockApp.tick() {
        clock.now += 60_000
    }

    @Test
    fun theWholeManualScriptOfLeva12() {
        val app = StockApp(dataDir)
        app.filaments.add(StockSamples.pla)

        // 1. Ligar o controle da cor com 1000 g, avisar abaixo de 300 g, carretel vazio de 200 g.
        app.stock.startTracking(red, 1000.0)
        app.stock.updateItemSettings(red, lowThreshold = 300.0, emptySpoolGrams = 200.0)
        app.tick()
        assertEquals(1000.0, app.balance(), 1e-6)

        // 2. Um pedido de 100 g: Aprovado reserva, Pronto desconta, voltar devolve.
        val first = app.history.saveOrder(StockSamples.quote(grams = 100.0))
        app.history.updateStatus(first.id, OrderStatus.APROVADO)
        app.tick()
        assertEquals(1000.0, app.balance(), 1e-6)
        assertEquals(100.0, app.reserved(), 1e-6)

        app.history.updateStatus(first.id, OrderStatus.PRONTO)
        app.tick()
        assertEquals(900.0, app.balance(), 1e-6, "Pronto desconta")
        assertEquals(0.0, app.reserved(), 1e-6)

        app.history.updateStatus(first.id, OrderStatus.APROVADO)
        app.tick()
        assertEquals(1000.0, app.balance(), 1e-6, "voltar pra Aprovado devolve")
        assertEquals(100.0, app.reserved(), 1e-6)

        // 3. Pronto de novo, Entregue e pago: a baixa fica, uma só.
        app.history.updateStatus(first.id, OrderStatus.PRONTO)
        app.tick()
        app.history.updateStatus(first.id, OrderStatus.ENTREGUE)
        app.tick()
        app.history.addPayment(first.id, 20.0, PaymentMethod.PIX, epochDay = 20_000)
        app.tick()
        assertEquals(900.0, app.balance(), 1e-6, "entregar e receber não mexem na baixa")
        assertEquals(1, app.stock.movements.value.count { it.kind == StockMovementKind.ORDER && it.quoteId == first.id })

        // 4. Outro pedido de 100 g pronto e excluído: o material foi gasto. Restaurar não desconta de novo.
        val second = app.history.saveOrder(StockSamples.quote(grams = 100.0))
        app.history.updateStatus(second.id, OrderStatus.PRONTO)
        app.tick()
        assertEquals(800.0, app.balance(), 1e-6)
        app.history.delete(second.id)
        app.tick()
        assertEquals(800.0, app.balance(), 1e-6, "excluir não devolve")
        app.history.restore(second.id)
        app.tick()
        assertEquals(800.0, app.balance(), 1e-6, "restaurar não desconta de novo")

        // 5. Consumo avulso de 50 g, depois pesar o carretel: 1100 g na balança menos 200 g do carretel vazio.
        app.stock.recordManualUse(red, 50.0, ManualUseReason.FAILURE, note = "Peça falhou")
        app.tick()
        assertEquals(750.0, app.balance(), 1e-6)
        app.stock.recordWeighing(red, scaleGrams = 1100.0, emptySpoolGrams = 200.0)
        app.tick()
        assertEquals(900.0, app.balance(), 1e-6, "o saldo é o que a balança disse")

        // 6. Um pedido aprovado de 700 g: 900 de saldo, 700 reservados e 300 de limite pedem 100 g na lista de compras.
        val third = app.history.saveOrder(StockSamples.quote(grams = 700.0))
        app.history.updateStatus(third.id, OrderStatus.APROVADO)
        app.tick()
        val line = app.shoppingList().lines.single()
        assertEquals(red, line.ref)
        assertEquals(100.0, line.amount, 1e-6)

        // 7. Compra de 2 kg por R$ 170 com R$ 13 de frete: o saldo sobe 2000 g e o preço do filamento vira R$ 91,50/kg.
        val purchase = Purchase(
            id = "compra-1",
            dateEpochDay = 20_000,
            supplier = "Loja de filamento",
            shipping = 13.0,
            lines = listOf(PurchaseLine(ref = red, amount = 2000.0, price = 170.0)),
        )
        val updates = purchase.catalogPriceUpdates(app.filaments.filaments.value, app.consumables.consumables.value)
        val priceUpdate = assertIs<CatalogPriceUpdate.FilamentPrice>(updates.single())
        assertEquals(89.90, priceUpdate.currentPricePerKg, 1e-9)
        assertEquals(91.50, priceUpdate.newPricePerKg, 1e-9)
        app.stock.recordPurchase(purchase, updates, app.filaments, app.consumables)
        app.tick()
        assertEquals(2900.0, app.balance(), 1e-6)
        assertEquals(91.50, app.pla().pricePerKg, 1e-9)
        assertTrue(app.shoppingList().lines.isEmpty(), "com a compra, nada falta")

        // 8. Abrir o app de novo: tudo estava gravado.
        val reopened = StockApp(dataDir)
        assertEquals(2900.0, reopened.balance(), 1e-6)
        assertEquals(700.0, reopened.reserved(), 1e-6)
        assertEquals(91.50, reopened.pla().pricePerKg, 1e-9)
        assertEquals(listOf("compra-1"), reopened.stock.purchases.value.map { it.id })
        assertEquals(300.0, reopened.stock.itemFor(red)?.lowThreshold)

        // 9. Exportar e importar: noutra instalação o filamento chega com o preço novo, sem saldo nem compras (decisão 164).
        val exported = reopened.filaments.exportFilamentsJson()
        val otherInstall = StockApp(createTempDirectory("3dreport-test-import").toFile())
        val imported = assertIs<FilamentImportPlan.Ready>(otherInstall.filaments.importFilaments(exported))
        assertEquals(listOf(StockSamples.pla.name), imported.toAdd.map { it.name }, "os filamentos de fábrica já estavam lá")
        val copy = otherInstall.filaments.filaments.value.single { it.name == StockSamples.pla.name }
        assertEquals(StockSamples.pla.name, copy.name)
        assertEquals(91.50, copy.pricePerKg, 1e-9)
        assertEquals(StockSamples.pla.colors.map { it.name }, copy.colors.map { it.name })
        assertTrue(otherInstall.stock.items.value.isEmpty())
        assertTrue(otherInstall.stock.movements.value.isEmpty())

        // E importar no mesmo app não duplica.
        val countBefore = reopened.filaments.filaments.value.size
        val again = assertIs<FilamentImportPlan.Ready>(reopened.filaments.importFilaments(exported))
        assertTrue(again.toAdd.isEmpty())
        assertEquals(countBefore, again.alreadyPresent)
        assertEquals(countBefore, reopened.filaments.filaments.value.size)
    }
}
