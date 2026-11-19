package com.threedreport.core.report

import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StatusChange
import com.threedreport.core.model.StockMovement
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.StockFixtures.black
import com.threedreport.core.report.StockFixtures.filaments
import com.threedreport.core.report.StockFixtures.item
import com.threedreport.core.report.StockFixtures.order
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

class StockLedgerTest {

    /** Controle de PLA vermelho e preto desde o instante 100. */
    private val items = listOf(item(plaRed, since = 100L), item(plaBlack, since = 100L))

    private var nextId = 0
    private val newId: () -> String = { "new-${nextId++}" }

    private fun reconcile(previous: List<SavedQuote>, current: List<SavedQuote>, movements: List<StockMovement> = emptyList(), now: Long = 500L) =
        StockLedger.reconcile(previous, current, movements, items, filaments, now, newId)

    private fun aprovado(id: String = "q", grams: Double = 100.0) =
        order(id, OrderStatus.APROVADO, quote(usages = listOf(usage(pla, grams, red))), number = 42, history = listOf(StatusChange(OrderStatus.ORCADO, 150L), StatusChange(OrderStatus.APROVADO, 160L)))

    private fun SavedQuote.movedTo(status: OrderStatus, at: Long = 200L) = withStatus(status, at)

    private fun List<StockMovement>.orderMovements() = filter { it.kind == StockMovementKind.ORDER }

    @Test
    fun anOrderThatEntersReadyGetsOneMovementPerTrackedItem() {
        val before = aprovado()
        val after = before.movedTo(OrderStatus.PRONTO)

        val movement = reconcile(listOf(before), listOf(after)).single()

        assertEquals(plaRed, movement.ref)
        assertEquals(-100.0, movement.amount, 1e-6)
        assertEquals(StockMovementKind.ORDER, movement.kind)
        assertEquals("q", movement.quoteId)
        assertEquals(42, movement.orderNumber)
        assertEquals(500L, movement.epochMillis)
    }

    @Test
    fun deliveredAlsoCountsAndReadyToDeliveredChangesNothing() {
        val before = aprovado()
        val delivered = before.movedTo(OrderStatus.ENTREGUE)
        assertEquals(1, reconcile(listOf(before), listOf(delivered)).size)

        val ready = before.movedTo(OrderStatus.PRONTO)
        val first = reconcile(listOf(before), listOf(ready))
        val second = reconcile(listOf(ready), listOf(ready.movedTo(OrderStatus.ENTREGUE, 300L)), first)

        assertEquals(first, second)
    }

    @Test
    fun anOrderThatLeavesReadyLosesTheMovement() {
        val before = aprovado()
        val ready = before.movedTo(OrderStatus.PRONTO)
        val movements = reconcile(listOf(before), listOf(ready))

        assertTrue(reconcile(listOf(ready), listOf(ready.movedTo(OrderStatus.APROVADO, 300L)), movements).isEmpty())
        assertTrue(reconcile(listOf(ready), listOf(ready.movedTo(OrderStatus.CANCELADO, 300L)), movements).isEmpty())
    }

    @Test
    fun editingAReadyOrderRedoesTheMovementWithTheNewConsumption() {
        val before = aprovado()
        val ready = before.movedTo(OrderStatus.PRONTO)
        val movements = reconcile(listOf(before), listOf(ready))
        val edited = ready.copy(quote = quote(usages = listOf(usage(pla, 150.0, red), usage(pla, 30.0, black))), lastEditedEpochMillis = 400L)

        val redone = reconcile(listOf(ready), listOf(edited), movements)

        assertEquals(2, redone.size)
        val redMovement = redone.single { it.ref == plaRed }
        assertEquals(movements.single().id, redMovement.id, "o movimento é refeito, não trocado")
        assertEquals(-150.0, redMovement.amount, 1e-6)
        assertEquals(-30.0, redone.single { it.ref == plaBlack }.amount, 1e-6)
    }

    @Test
    fun editingThatDropsAColorRemovesItsMovement() {
        val before = order("q", OrderStatus.APROVADO, quote(usages = listOf(usage(pla, 10.0, red), usage(pla, 20.0, black))), history = listOf(StatusChange(OrderStatus.APROVADO, 150L)))
        val ready = before.movedTo(OrderStatus.PRONTO)
        val movements = reconcile(listOf(before), listOf(ready))
        val edited = ready.copy(quote = quote(usages = listOf(usage(pla, 10.0, red))))

        assertEquals(listOf(plaRed), reconcile(listOf(ready), listOf(edited), movements).map { it.ref })
    }

    @Test
    fun aDeletedOrderKeepsTheMovementAndRestoringDoesNotCountAgain() {
        val before = aprovado()
        val ready = before.movedTo(OrderStatus.PRONTO)
        val movements = reconcile(listOf(before), listOf(ready))

        val afterDelete = reconcile(listOf(ready), emptyList(), movements)
        assertEquals(movements, afterDelete)

        val afterRestore = reconcile(emptyList(), listOf(ready), afterDelete)
        assertEquals(movements, afterRestore)
    }

    @Test
    fun anOrderAlreadyReadyBeforeTrackingStartedNeverCounts() {
        // Pronto no instante 50, antes do controle começar (100).
        val old = order("old", OrderStatus.PRONTO, quote(usages = listOf(usage(pla, 100.0, red))), history = listOf(StatusChange(OrderStatus.ORCADO, 10L), StatusChange(OrderStatus.PRONTO, 50L)))

        // Sem mudança: nada.
        assertTrue(reconcile(listOf(old), listOf(old)).isEmpty())
        // Pronto para Entregue: continua sem descontar.
        assertTrue(reconcile(listOf(old), listOf(old.movedTo(OrderStatus.ENTREGUE, 300L))).isEmpty())
        // Editado: continua sem descontar.
        assertTrue(reconcile(listOf(old), listOf(old.copy(quote = quote(usages = listOf(usage(pla, 300.0, red)))))).isEmpty())
        // Excluído e restaurado da lixeira: continua sem descontar.
        assertTrue(reconcile(emptyList(), listOf(old)).isEmpty())
    }

    @Test
    fun anOldOrderWithoutHistoryUsesItsCreationDate() {
        val legacy = order("legacy", OrderStatus.ENTREGUE, history = emptyList(), savedAt = 10L)

        assertTrue(reconcile(emptyList(), listOf(legacy)).isEmpty())
        assertEquals(10L, StockLedger.enteredPrintedAtEpochMillis(legacy))
    }

    @Test
    fun anOrderThatLeavesAndComesBackAfterTrackingStartsCounts() {
        val old = order("old", OrderStatus.PRONTO, quote(usages = listOf(usage(pla, 100.0, red))), history = listOf(StatusChange(OrderStatus.PRONTO, 50L)))
        val back = old.movedTo(OrderStatus.APROVADO, 200L)
        assertTrue(reconcile(listOf(old), listOf(back)).isEmpty())

        val readyAgain = back.movedTo(OrderStatus.PRONTO, 300L)
        assertEquals(-100.0, reconcile(listOf(back), listOf(readyAgain)).single().amount, 1e-6)
    }

    @Test
    fun anOrderCreatedAlreadyReadyAfterTrackingStartedCounts() {
        val created = order("new", OrderStatus.PRONTO, history = listOf(StatusChange(OrderStatus.PRONTO, 300L)), savedAt = 300L)

        assertEquals(1, reconcile(emptyList(), listOf(created)).size)
    }

    @Test
    fun catalogProductsNeverCount() {
        val product = order("prod", OrderStatus.APROVADO, kind = QuoteKind.PRODUCT, history = listOf(StatusChange(OrderStatus.APROVADO, 150L)))

        assertTrue(reconcile(listOf(product), listOf(product.movedTo(OrderStatus.PRONTO))).isEmpty())
    }

    @Test
    fun onlyTrackedItemsGetMovements() {
        val before = order("q", OrderStatus.APROVADO, quote(usages = listOf(usage(StockFixtures.petg, 40.0, null), usage(pla, 10.0, red))), history = listOf(StatusChange(OrderStatus.APROVADO, 150L)))

        val movements = reconcile(listOf(before), listOf(before.movedTo(OrderStatus.PRONTO)))

        assertEquals(listOf(plaRed), movements.map { it.ref })
    }

    @Test
    fun otherMovementsAreKeptAsTheyAre() {
        val manual = StockMovement(id = "m", ref = plaRed, epochMillis = 1L, amount = -5.0, kind = StockMovementKind.MANUAL_USE)
        val before = aprovado()

        val result = reconcile(listOf(before), listOf(before.movedTo(OrderStatus.PRONTO)), listOf(manual))

        assertEquals(manual, result.first())
        assertEquals(2, result.size)
    }

    @Test
    fun aChangeThatIsNotAboutStatusOrConsumptionChangesNothing() {
        val before = aprovado()
        val ready = before.movedTo(OrderStatus.PRONTO)
        val movements = reconcile(listOf(before), listOf(ready))

        assertEquals(movements, reconcile(listOf(ready), listOf(ready.copy(notes = "embalar bem")), movements))
    }

    // --- Início, ajuste e balança (itens 3 e 6) ---

    @Test
    fun startingRecordsTheInformedBalance() {
        val start = StockLedger.startMovement(plaRed, 1000.0, "s", 7L)

        assertEquals(StockMovementKind.START, start.kind)
        assertEquals(1000.0, start.amount)
        assertEquals(7L, start.epochMillis)
    }

    @Test
    fun adjustingRecordsTheDifferenceToTheNewBalance() {
        val movements = listOf(StockLedger.startMovement(plaRed, 1000.0, "s", 0L))

        val adjustment = StockLedger.adjustmentTo(plaRed, 640.0, movements, "a", 1L)!!

        assertEquals(StockMovementKind.ADJUSTMENT, adjustment.kind)
        assertEquals(-360.0, adjustment.amount, 1e-9)
        assertNull(StockLedger.adjustmentTo(plaRed, 1000.0, movements, "b", 1L), "saldo igual: nada a gravar")
    }

    @Test
    fun weighingSubtractsTheEmptySpool() {
        assertEquals(830.0, StockLedger.weighedBalance(scaleGrams = 1080.0, emptySpoolGrams = 250.0), 1e-9)
        assertEquals(1080.0, StockLedger.weighedBalance(scaleGrams = 1080.0, emptySpoolGrams = null), 1e-9)
    }

    @Test
    fun manualUseIsRecordedAsANegativeAmount() {
        val use = StockLedger.manualUse(plaRed, 35.0, com.threedreport.core.model.ManualUseReason.FAILURE, "  espaguete ", "u", 3L)

        assertEquals(-35.0, use.amount)
        assertEquals(com.threedreport.core.model.ManualUseReason.FAILURE, use.reason)
        assertEquals("espaguete", use.note)
        assertTrue(use.isRemovableByHand)
    }

    // --- Movimentos de compra (item 16) ---

    private val purchase = Purchase(id = "p", dateEpochDay = 0, lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0), PurchaseLine(plaRed, 500.0, 45.0), PurchaseLine(ring, 50.0, 12.0)))

    @Test
    fun aPurchaseAddsOneMovementPerTrackedItem() {
        val result = StockLedger.purchaseMovements(null, purchase, emptyList(), items, 9L, newId)

        val movement = result.single()
        assertEquals(plaRed, movement.ref)
        assertEquals(1500.0, movement.amount, 1e-9)
        assertEquals(StockMovementKind.PURCHASE, movement.kind)
        assertEquals("p", movement.purchaseId)
    }

    @Test
    fun editingAPurchaseRedoesItsMovements() {
        val first = StockLedger.purchaseMovements(null, purchase, emptyList(), items, 9L, newId)
        val edited = purchase.copy(lines = listOf(PurchaseLine(plaRed, 750.0, 70.0), PurchaseLine(plaBlack, 1000.0, 90.0)))

        val result = StockLedger.purchaseMovements(purchase, edited, first, items, 10L, newId)

        assertEquals(first.single().id, result.single { it.ref == plaRed }.id)
        assertEquals(750.0, result.single { it.ref == plaRed }.amount, 1e-9)
        assertEquals(1000.0, result.single { it.ref == plaBlack }.amount, 1e-9)
    }

    @Test
    fun editingAnOldPurchaseOfAnItemTrackedLaterDoesNotAddItAgain() {
        // Comprou antes de controlar: a compra não somou gramas, e o saldo informado ao ativar já inclui ela.
        val before = Purchase(id = "p", dateEpochDay = 0, lines = listOf(PurchaseLine(plaRed, 1000.0, 90.0)))
        val edited = before.copy(note = "nota fiscal 123")

        assertTrue(StockLedger.purchaseMovements(before, edited, emptyList(), items, 10L, newId).isEmpty())
    }

    @Test
    fun deletingAPurchaseRemovesItsMovements() {
        val manual = StockMovement(id = "m", ref = plaRed, epochMillis = 1L, amount = -5.0, kind = StockMovementKind.MANUAL_USE)
        val movements = StockLedger.purchaseMovements(null, purchase, listOf(manual), items, 9L, newId)

        assertEquals(listOf(manual), StockLedger.withoutPurchase("p", movements))
    }
}
