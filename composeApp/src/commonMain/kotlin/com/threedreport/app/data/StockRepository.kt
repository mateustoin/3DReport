package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.CatalogPriceUpdate
import com.threedreport.core.model.Filament
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovement
import com.threedreport.core.report.StockLedger
import com.threedreport.core.report.StockReport
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Estoque e compras (Leva 12, decisões 158 a 162): o controle de saldo ligado em cada item, os movimentos que somam o
 * saldo e as compras. As contas (saldo, reservado, alertas, lista de compras) são do `core`
 * ([com.threedreport.core.report.StockReport]); aqui só se grava.
 *
 * A baixa dos pedidos não é chamada pelas telas: o histórico montado com [reconcilingStock] chama [reconcileOrders] a
 * cada mudança. Do mesmo jeito, o cadastro de filamentos montado com [deletingStockWith] chama [deleteAllFor] quando uma
 * cor sai do filamento. Excluir o filamento ou o insumo inteiro não apaga nada: ele vai pra lixeira, e restaurar traz o
 * saldo de volta.
 */
interface StockRepository {
    /** Os itens com controle de saldo ligado. */
    val items: StateFlow<List<StockItem>>

    /** Todos os movimentos, de todos os itens, na ordem em que foram gravados. */
    val movements: StateFlow<List<StockMovement>>

    /** As compras, na ordem em que foram registradas (a tela ordena por data). */
    val purchases: StateFlow<List<Purchase>>

    /** O controle de [ref], ou `null` se o item não é controlado. */
    fun itemFor(ref: StockItemRef): StockItem?

    /**
     * "Controlar saldo": liga o controle de [ref] com o saldo atual [balance] (gramas ou unidades), gravado como
     * movimento de início. Se já era controlado, devolve o controle que existe, sem gravar nada.
     */
    fun startTracking(ref: StockItemRef, balance: Double): StockItem

    /** "Parar de controlar": apaga o controle e os movimentos de [ref]. As compras ficam. */
    fun stopTracking(ref: StockItemRef)

    /** Troca o "Avisar abaixo de" e o carretel vazio de [ref] (`null` = sem). Não faz nada sem controle. */
    fun updateItemSettings(ref: StockItemRef, lowThreshold: Double?, emptySpoolGrams: Double?)

    /** Consumo avulso de [amount] (positivo). `null`, sem gravar, se [ref] não é controlado ou [amount] não é positivo. */
    fun recordManualUse(ref: StockItemRef, amount: Double, reason: ManualUseReason, note: String?): StockMovement?

    /** "Corrigir saldo": grava a diferença até [newBalance]. `null` sem controle ou quando o saldo já é esse. */
    fun adjustTo(ref: StockItemRef, newBalance: Double, note: String? = null): StockMovement?

    /**
     * "Pesei o carretel": o saldo passa a ser [scaleGrams] menos [emptySpoolGrams] ([StockLedger.weighedBalance]), e o
     * carretel vazio fica lembrado no item pra próxima vez. `null` sem controle ou quando o saldo já é esse.
     */
    fun recordWeighing(ref: StockItemRef, scaleGrams: Double, emptySpoolGrams: Double?): StockMovement?

    /** Exclui um consumo avulso ou ajuste, desfazendo o efeito dele. `false` pra outro tipo ou id que não existe. */
    fun deleteMovement(id: String): Boolean

    /**
     * Grava [purchase] (nova, ou a edição da de mesmo id) e refaz os movimentos dela nos itens controlados
     * ([StockLedger.purchaseMovements]). O fornecedor vai com a grafia de um já usado ([StockReport.normalizedSupplier]),
     * e link e observação em branco viram `null`. Devolve a compra como foi gravada. Não mexe no cadastro: pra marcar a
     * cor como "tem" e atualizar preço, use [recordPurchase].
     */
    fun savePurchase(purchase: Purchase): Purchase

    /** Exclui a compra [id] e os movimentos dela. */
    fun deletePurchase(id: String)

    /** Os fornecedores já usados, pra sugestão no formulário. */
    fun supplierSuggestions(): List<String>

    /**
     * Apaga o controle e os movimentos de [ref] (uma cor saiu do filamento, e a cor não tem lixeira). Quem exclui o
     * filamento ou o insumo inteiro não chama: o controle fica pra quando ele for restaurado.
     */
    fun deleteAllFor(ref: StockItemRef)

    /**
     * A baixa dos pedidos ([StockLedger.reconcile]) entre a lista de pedidos [previous], a última vista, e [current].
     * Quem chama é o histórico montado com [reconcilingStock].
     */
    fun reconcileOrders(previous: List<SavedQuote>, current: List<SavedQuote>, filaments: List<Filament>)
}

/**
 * [StockRepository] em três listas de registros (decisão 106): `stock-items.json`, `stock-movements.json` e
 * `purchases.json`. Nenhum campo novo nos cadastros: filamentos e insumos continuam no formato da 2.2.
 */
@OptIn(ExperimentalUuidApi::class)
class StoredStockRepository(
    private val itemRecords: RecordCollection<StockItem>,
    private val movementRecords: RecordCollection<StockMovement>,
    private val purchaseRecords: RecordCollection<Purchase>,
    private val clock: Clock,
    private val newId: () -> String = { Uuid.random().toString() },
) : StockRepository {

    override val items: StateFlow<List<StockItem>> = itemRecords.items
    override val movements: StateFlow<List<StockMovement>> = movementRecords.items
    override val purchases: StateFlow<List<Purchase>> = purchaseRecords.items

    override fun itemFor(ref: StockItemRef): StockItem? = items.value.find { it.ref == ref }

    override fun startTracking(ref: StockItemRef, balance: Double): StockItem {
        itemFor(ref)?.let { return it }
        require(balance.isFinite()) { "balance precisa ser um número: $balance" }
        val now = clock.nowMillis()
        val item = StockItem(id = newId(), ref = ref, trackedSinceEpochMillis = now)
        itemRecords.add(item)
        movementRecords.add(StockLedger.startMovement(ref, balance, newId(), now))
        return item
    }

    override fun stopTracking(ref: StockItemRef) = deleteWhere { it == ref }

    override fun updateItemSettings(ref: StockItemRef, lowThreshold: Double?, emptySpoolGrams: Double?) {
        val item = itemFor(ref) ?: return
        itemRecords.update(item.copy(lowThreshold = lowThreshold.positiveOrNull(), emptySpoolGrams = emptySpoolGrams.positiveOrNull()))
    }

    override fun recordManualUse(ref: StockItemRef, amount: Double, reason: ManualUseReason, note: String?): StockMovement? {
        if (itemFor(ref) == null || !(amount > 0) || !amount.isFinite()) return null
        return StockLedger.manualUse(ref, amount, reason, note, newId(), clock.nowMillis()).also(movementRecords::add)
    }

    override fun adjustTo(ref: StockItemRef, newBalance: Double, note: String?): StockMovement? {
        if (itemFor(ref) == null || !newBalance.isFinite()) return null
        return StockLedger.adjustmentTo(ref, newBalance, movements.value, newId(), clock.nowMillis(), note)?.also(movementRecords::add)
    }

    override fun recordWeighing(ref: StockItemRef, scaleGrams: Double, emptySpoolGrams: Double?): StockMovement? {
        val item = itemFor(ref) ?: return null
        val spool = emptySpoolGrams.positiveOrNull()
        itemRecords.update(item.copy(emptySpoolGrams = spool))
        return adjustTo(ref, StockLedger.weighedBalance(scaleGrams, spool))
    }

    override fun deleteMovement(id: String): Boolean {
        val movement = movementRecords.find(id) ?: return false
        if (!movement.isRemovableByHand) return false
        return movementRecords.delete(id)
    }

    override fun savePurchase(purchase: Purchase): Purchase {
        val previous = purchaseRecords.find(purchase.id)
        val clean = purchase.copy(
            supplier = StockReport.normalizedSupplier(purchase.supplier, purchases.value, excludingPurchaseId = purchase.id),
            link = purchase.link.clean(),
            note = purchase.note.clean(),
        )
        if (previous == null) purchaseRecords.add(clean) else purchaseRecords.update(clean)
        applyMovements(StockLedger.purchaseMovements(previous, clean, movements.value, items.value, clock.nowMillis(), newId))
        return clean
    }

    override fun deletePurchase(id: String) {
        if (!purchaseRecords.delete(id)) return
        applyMovements(StockLedger.withoutPurchase(id, movements.value))
    }

    override fun supplierSuggestions(): List<String> = StockReport.suppliers(purchases.value)

    override fun deleteAllFor(ref: StockItemRef) = deleteWhere { it == ref }

    override fun reconcileOrders(previous: List<SavedQuote>, current: List<SavedQuote>, filaments: List<Filament>) {
        if (items.value.isEmpty() && movements.value.isEmpty()) return
        applyMovements(StockLedger.reconcile(previous, current, movements.value, items.value, filaments, clock.nowMillis(), newId))
    }

    private fun deleteWhere(matches: (StockItemRef) -> Boolean) {
        itemRecords.applyBatch(deleted = items.value.filter { matches(it.ref) }.map { it.id })
        movementRecords.applyBatch(deleted = movements.value.filter { matches(it.ref) }.map { it.id })
    }

    /**
     * Grava a diferença entre os movimentos de agora e [target] num lote só ([RecordCollection.applyBatch]): exclui quem
     * saiu, troca quem mudou, acrescenta quem entrou.
     */
    private fun applyMovements(target: List<StockMovement>) {
        val before = movements.value
        if (target == before) return
        val beforeById = before.associateBy { it.id }
        val targetIds = target.mapTo(HashSet()) { it.id }
        movementRecords.applyBatch(
            added = target.filter { it.id !in beforeById },
            updated = target.filter { movement -> beforeById[movement.id].let { it != null && it != movement } },
            deleted = before.filter { it.id !in targetIds }.map { it.id },
        )
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

    private fun Double?.positiveOrNull(): Double? = this?.takeIf { it > 0 && it.isFinite() }
}

/**
 * "Salvar" do formulário de compra (itens 16 e 17): grava [purchase] no estoque, marca como "tem" as cores sem controle
 * que entraram nesta compra (as que já estavam numa versão anterior dela ficam como estão) e aplica os [priceUpdates]
 * que a pessoa deixou marcados ([Purchase.catalogPriceUpdates]), lendo o cadastro de agora. Pedido salvo nunca muda.
 */
fun StockRepository.recordPurchase(
    purchase: Purchase,
    priceUpdates: List<CatalogPriceUpdate>,
    filaments: FilamentRepository,
    consumables: ConsumableRepository,
): Purchase {
    val previousRefs = purchases.value.find { it.id == purchase.id }?.lines?.mapTo(HashSet()) { it.ref }.orEmpty()
    val saved = savePurchase(purchase)
    val tracked = items.value.mapTo(HashSet()) { it.ref }
    val restocked = saved.lines.map { it.ref }
        .filterIsInstance<StockItemRef.ColorRef>()
        .filter { it !in tracked && it !in previousRefs }
        .groupBy({ it.filamentId }, { it.colorId })
    for ((filamentId, colorIds) in restocked) {
        val filament = filaments.filaments.value.find { it.id == filamentId } ?: continue
        filaments.update(filament.copy(colors = filament.colors.map { if (it.id in colorIds) it.copy(inStock = true) else it }))
    }
    for (update in priceUpdates) {
        when (update) {
            is CatalogPriceUpdate.FilamentPrice -> filaments.filaments.value.find { it.id == update.filamentId }?.let { filaments.update(update.applyTo(it)) }
            is CatalogPriceUpdate.ConsumableCost -> consumables.consumables.value.find { it.id == update.consumableId }?.let { consumables.update(update.applyTo(it)) }
        }
    }
    return saved
}
