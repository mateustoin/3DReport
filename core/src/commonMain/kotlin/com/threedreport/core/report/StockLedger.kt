package com.threedreport.core.report

import com.threedreport.core.model.Filament
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovement
import com.threedreport.core.model.StockMovementKind
import kotlin.math.abs

/**
 * Os movimentos que o estoque grava (decisão 158): a baixa dos pedidos por transição de andamento, a de compra, o
 * início, o ajuste e o consumo avulso. Funções puras: recebem a lista de movimentos e devolvem a nova, e quem grava é
 * o repositório. A leitura (saldo, reservado, alertas) fica em [StockReport].
 */
object StockLedger {

    /**
     * A baixa dos pedidos (item 4 da Leva 12): compara cada pedido de [current] com ele mesmo em [previous] (a última
     * lista vista) e devolve [movements] com os movimentos de pedido acertados. Os outros movimentos ficam como estão.
     *
     * - Entrou em Pronto/Entregue (de outro andamento, ou criado já assim): ganha um movimento por item controlado que
     *   usa ([StockReport.consumptionOf]).
     * - Saiu de Pronto/Entregue e continua existindo (voltou, Cancelado): perde os movimentos, e o material volta.
     * - Continua Pronto/Entregue com consumo diferente (reaberto e salvo): os movimentos são refeitos, com o mesmo id e
     *   a mesma data.
     * - Sumiu da lista (excluído): os movimentos ficam, porque o material foi gasto. Restaurar não desconta de novo.
     * - Sem mudança, ou Pronto para Entregue: nada.
     * - Produto do catálogo nunca desconta.
     *
     * Um pedido que já estava pronto quando o controle de um item começou ([StockItem.trackedSinceEpochMillis]) não
     * desconta desse item, nem se for editado, restaurado da lixeira ou entregue depois: o saldo informado no início já
     * está "depois" dele. Ele volta a descontar se sair de pronto e entrar de novo. A primeira lista ao abrir o app é só
     * a base: quem chama começa com [previous] = [current].
     *
     * [newId] dá o id de cada movimento novo; [nowMillis] é a data dele.
     */
    fun reconcile(
        previous: List<SavedQuote>,
        current: List<SavedQuote>,
        movements: List<StockMovement>,
        items: List<StockItem>,
        filaments: List<Filament>,
        nowMillis: Long,
        newId: () -> String,
    ): List<StockMovement> {
        val previousById = previous.associateBy { it.id }
        val itemsByRef = items.associateBy { it.ref }
        var result = movements
        for (quote in current) {
            val before = previousById[quote.id]
            if (before == quote) continue
            val existing = result.filter { it.kind == StockMovementKind.ORDER && it.quoteId == quote.id }
            val desired = desiredOrderMovements(quote, before, existing, itemsByRef, filaments)
            result = syncMovements(result, existing, desired) { ref, amount ->
                StockMovement(
                    id = newId(),
                    ref = ref,
                    epochMillis = nowMillis,
                    amount = amount,
                    kind = StockMovementKind.ORDER,
                    quoteId = quote.id,
                    orderNumber = quote.number.takeIf { it > 0 },
                )
            }
        }
        return result
    }

    /** Quanto o pedido [quote] deve ter descontado de cada item, com sinal (negativo). */
    private fun desiredOrderMovements(
        quote: SavedQuote,
        before: SavedQuote?,
        existing: List<StockMovement>,
        itemsByRef: Map<StockItemRef, StockItem>,
        filaments: List<Filament>,
    ): Map<StockItemRef, Double> {
        if (!quote.isPrinted) return emptyMap()
        val justEntered = before != null && !before.isPrinted
        val enteredAt = enteredPrintedAtEpochMillis(quote) ?: return emptyMap()
        val alreadyCounted = existing.mapTo(HashSet()) { it.ref }
        return StockReport.consumptionOf(quote, filaments)
            .filter { (ref, _) ->
                val item = itemsByRef[ref] ?: return@filter false
                justEntered || ref in alreadyCounted || item.trackedSinceEpochMillis <= enteredAt
            }
            .mapValues { -it.value }
    }

    private val SavedQuote.isPrinted: Boolean
        get() = isOrder && status in StockReport.PRINTED_STATUSES

    /**
     * Quando [quote] entrou em Pronto/Entregue pela última vez, vindo de fora deles (Pronto para Entregue não conta como
     * entrar de novo). `null` se não está pronto. Pedido sem essa mudança no histórico (gravado antes do histórico
     * existir) usa a data de criação.
     */
    fun enteredPrintedAtEpochMillis(quote: SavedQuote): Long? {
        if (quote.status !in StockReport.PRINTED_STATUSES) return null
        var enteredAt: Long? = null
        var wasPrinted = false
        for (change in quote.statusHistory) {
            val printed = change.status in StockReport.PRINTED_STATUSES
            if (printed && !wasPrinted) enteredAt = change.atEpochMillis
            wasPrinted = printed
        }
        return enteredAt ?: quote.savedAtEpochMillis
    }

    /**
     * Os movimentos de compra depois de gravar [purchase] (item 16), sendo [previous] a mesma compra antes da edição (ou
     * `null` numa compra nova). Cada item controlado da compra tem um movimento, com as linhas repetidas somadas; editar
     * refaz o movimento (mesmo id e data) e tira o de item que saiu da compra.
     *
     * Um item que já estava na compra antes da edição e não tinha movimento (não era controlado quando ela foi gravada)
     * continua sem: o saldo informado ao ligar o controle já inclui essa compra.
     */
    fun purchaseMovements(
        previous: Purchase?,
        purchase: Purchase,
        movements: List<StockMovement>,
        items: List<StockItem>,
        nowMillis: Long,
        newId: () -> String,
    ): List<StockMovement> {
        val tracked = items.mapTo(HashSet()) { it.ref }
        val existing = movements.filter { it.kind == StockMovementKind.PURCHASE && it.purchaseId == purchase.id }
        val counted = existing.mapTo(HashSet()) { it.ref }
        val before = previous?.amountByRef()?.keys.orEmpty()
        val desired = purchase.amountByRef().filterKeys { it in tracked && (it in counted || it !in before) }
        return syncMovements(movements, existing, desired) { ref, amount ->
            StockMovement(id = newId(), ref = ref, epochMillis = nowMillis, amount = amount, kind = StockMovementKind.PURCHASE, purchaseId = purchase.id)
        }
    }

    /** [movements] sem os da compra [purchaseId] (excluir a compra). */
    fun withoutPurchase(purchaseId: String, movements: List<StockMovement>): List<StockMovement> =
        movements.filterNot { it.kind == StockMovementKind.PURCHASE && it.purchaseId == purchaseId }

    /** O movimento de início, com o saldo informado ao ligar o controle. */
    fun startMovement(ref: StockItemRef, amount: Double, id: String, nowMillis: Long): StockMovement =
        StockMovement(id = id, ref = ref, epochMillis = nowMillis, amount = amount, kind = StockMovementKind.START)

    /**
     * O ajuste que leva o saldo de [ref] a [newBalance] ("Corrigir saldo" e "Pesei o carretel"): a diferença entre o
     * novo e o de agora. `null` quando o saldo já é esse.
     */
    fun adjustmentTo(ref: StockItemRef, newBalance: Double, movements: List<StockMovement>, id: String, nowMillis: Long, note: String? = null): StockMovement? {
        val difference = newBalance - StockReport.balance(ref, movements)
        if (abs(difference) < StockReport.EPSILON) return null
        return StockMovement(id = id, ref = ref, epochMillis = nowMillis, amount = difference, kind = StockMovementKind.ADJUSTMENT, note = note.clean())
    }

    /** Saldo pela balança (item 6): o peso medido menos o carretel vazio. Sem carretel vazio, o peso é o saldo. */
    fun weighedBalance(scaleGrams: Double, emptySpoolGrams: Double?): Double = scaleGrams - (emptySpoolGrams ?: 0.0)

    /** Um consumo avulso de [amount] (positivo, em gramas ou unidades), gravado como movimento negativo. */
    fun manualUse(ref: StockItemRef, amount: Double, reason: ManualUseReason, note: String?, id: String, nowMillis: Long): StockMovement {
        require(amount > 0) { "amount deve ser positivo: $amount" }
        return StockMovement(id = id, ref = ref, epochMillis = nowMillis, amount = -amount, kind = StockMovementKind.MANUAL_USE, reason = reason, note = note.clean())
    }

    /**
     * Acerta [existing] (os movimentos de um pedido ou de uma compra, dentro de [movements]) pra [desired]: muda a
     * quantidade de quem continua (mantendo id e data), tira quem saiu e acrescenta no fim quem entrou.
     */
    private fun syncMovements(
        movements: List<StockMovement>,
        existing: List<StockMovement>,
        desired: Map<StockItemRef, Double>,
        create: (StockItemRef, Double) -> StockMovement,
    ): List<StockMovement> {
        if (existing.isEmpty() && desired.isEmpty()) return movements
        val existingIds = existing.mapTo(HashSet()) { it.id }
        val kept = HashSet<StockItemRef>()
        val updated = movements.mapNotNull { movement ->
            if (movement.id !in existingIds) return@mapNotNull movement
            val amount = desired[movement.ref]
            if (amount == null || !kept.add(movement.ref)) {
                null
            } else if (abs(amount - movement.amount) < StockReport.EPSILON) {
                movement
            } else {
                movement.copy(amount = amount)
            }
        }
        return updated + desired.filterKeys { it !in kept }.map { (ref, amount) -> create(ref, amount) }
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}
