package com.threedreport.core.report

import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Purchase
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItem
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovement
import com.threedreport.core.model.catalogFactor

/** Situação do saldo de um item controlado (decisão 159). */
enum class StockState {
    OK,

    /** Abaixo do "Avisar abaixo de" ([StockItem.lowThreshold]). */
    LOW,

    /** Saldo zero ou negativo, com ou sem limite. */
    OUT,
}

/**
 * Um item controlado com os números do momento.
 *
 * @property reserved o que os pedidos Aprovados e Em impressão ainda vão gastar dele.
 * @property available [balance] menos [reserved]; pode ser negativo.
 */
data class StockStatus(
    val item: StockItem,
    val balance: Double,
    val reserved: Double,
    val state: StockState,
) {
    val available: Double
        get() = balance - reserved
}

/**
 * A tela Estoque de uma vez (item 27): os itens controlados com os números ([tracked]), os que não têm controle
 * ([untracked]) e quantos pedem atenção ([alertCount], o selo da barra lateral). Filamentos e insumos arquivados ou
 * excluídos ficam de fora das três coisas. Ordem: filamentos por nome (depois marca), cores por nome, depois insumos
 * por nome.
 */
data class StockOverview(
    val tracked: List<StockStatus>,
    val untracked: List<StockItemRef>,
) {
    /** Quantos itens controlados estão baixos ou acabando. */
    val alertCount: Int
        get() = tracked.count { it.state != StockState.OK }
}

/**
 * Um orçamento que usa mais do que sobra livre de um item (item 13): "este pedido usa [needed] e sobram [available]
 * livres ([reserved] já reservados pra pedidos aprovados)".
 */
data class StockShortage(
    val ref: StockItemRef,
    val needed: Double,
    val available: Double,
    val reserved: Double,
)

/**
 * Uma linha da lista de compras (item 20): comprar [amount] = reservado + limite − saldo, com o porquê.
 *
 * @property lowThreshold o limite do item, ou `null` sem limite (conta zero na conta).
 */
data class ShoppingLine(
    val ref: StockItemRef,
    val amount: Double,
    val reserved: Double,
    val balance: Double,
    val lowThreshold: Double?,
)

/** Uma cor sem controle marcada "Acabou" que [orderCount] pedidos Aprovados ou Em impressão usam (item 21). */
data class OutOfStockColor(
    val ref: StockItemRef.ColorRef,
    val orderCount: Int,
)

/** A lista de compras (decisão 161): os itens controlados a comprar e, à parte, as cores sem controle esgotadas. */
data class ShoppingList(
    val lines: List<ShoppingLine>,
    val outOfStockColors: List<OutOfStockColor>,
) {
    val isEmpty: Boolean
        get() = lines.isEmpty() && outOfStockColors.isEmpty()

    /** Quantos itens a lista tem, pro "Lista de compras (N)". */
    val size: Int
        get() = lines.size + outOfStockColors.size
}

/**
 * Saldo, reservado, alertas e lista de compras do estoque (decisões 158 a 162). Funções puras e sem estado, no estilo
 * de [MaintenanceReport]. A baixa dos pedidos, que grava movimentos, fica em [StockLedger].
 */
object StockReport {

    /** Andamentos em que o material já foi gasto: os mesmos da manutenção. */
    val PRINTED_STATUSES: Set<OrderStatus> = MaintenanceReport.PRINTED_STATUSES

    /** Andamentos em que o material ainda vai ser gasto: entram no reservado. */
    val RESERVED_STATUSES: Set<OrderStatus> = setOf(OrderStatus.APROVADO, OrderStatus.EM_IMPRESSAO)

    /** Abaixo disto (em gramas ou unidades), a diferença é arredondamento de `Double`, e não material. */
    const val EPSILON = 1e-6

    /**
     * Quanto [quote] gasta de cada item (item 5). Filamento: o consumo de cada filamento e cor do pedido inteiro
     * ([Quote.filamentTotals]: o peso de cada uso, pelo retrato gravado no orçamento, vezes as rodadas da impressão e a
     * quantidade do pedido), atribuído a uma cor do estoque por [colorRefOf]. Insumo: a quantidade dele, vezes a
     * quantidade do pedido quando é por peça. A reserva de falha não entra: é preço, não material.
     *
     * Não olha se é pedido ou produto: quem decide se desconta é quem chama.
     */
    fun consumptionOf(quote: Quote, filaments: List<Filament>): Map<StockItemRef, Double> {
        val result = LinkedHashMap<StockItemRef, Double>()
        fun add(ref: StockItemRef, amount: Double) {
            if (amount > 0) result[ref] = (result[ref] ?: 0.0) + amount
        }
        for (total in quote.filamentTotals()) {
            val ref = colorRefOf(total.filament.id, total.color?.id, filaments) ?: continue
            add(ref, total.weightGrams)
        }
        for (consumable in quote.consumables) {
            add(StockItemRef.ConsumableRef(consumable.id), consumable.quantity * (if (consumable.chargedPerOrder) 1 else quote.quantity))
        }
        return result
    }

    /** [consumptionOf] do retrato de [savedQuote]. */
    fun consumptionOf(savedQuote: SavedQuote, filaments: List<Filament>): Map<StockItemRef, Double> = consumptionOf(savedQuote.quote, filaments)

    /**
     * A cor de [usage]: a gravada nele, ou, sem cor gravada, a única cor do filamento no cadastro ([filaments]). Com
     * várias cores e nenhuma gravada (ou filamento que saiu do cadastro), não é atribuída a nenhuma.
     */
    fun colorRefOf(usage: FilamentUsage, filaments: List<Filament>): StockItemRef.ColorRef? = colorRefOf(usage.filament.id, usage.color?.id, filaments)

    private fun colorRefOf(filamentId: String, colorId: String?, filaments: List<Filament>): StockItemRef.ColorRef? {
        val resolved = colorId ?: filaments.find { it.id == filamentId }?.colors?.singleOrNull()?.id ?: return null
        return StockItemRef.ColorRef(filamentId, resolved)
    }

    /** Saldo de [ref]: a soma dos movimentos dele. */
    fun balance(ref: StockItemRef, movements: List<StockMovement>): Double = movements.filter { it.ref == ref }.sumOf { it.amount }

    /** Saldo de cada item que tem movimento. */
    fun balances(movements: List<StockMovement>): Map<StockItemRef, Double> {
        val result = HashMap<StockItemRef, Double>()
        movements.forEach { result[it.ref] = (result[it.ref] ?: 0.0) + it.amount }
        return result
    }

    /** Os pedidos que reservam material: Aprovados e Em impressão, sem produto do catálogo. */
    fun reservingOrders(savedQuotes: List<SavedQuote>, excludingQuoteId: String? = null): List<SavedQuote> =
        savedQuotes.filter { it.isOrder && it.status in RESERVED_STATUSES && it.id != excludingQuoteId }

    /**
     * O que os pedidos Aprovados e Em impressão ainda vão gastar de cada item (item 11). [excludingQuoteId] tira um
     * pedido da conta: o que está sendo editado no Orçamento, pra não contar duas vezes.
     */
    fun reservedByRef(savedQuotes: List<SavedQuote>, filaments: List<Filament>, excludingQuoteId: String? = null): Map<StockItemRef, Double> {
        val result = HashMap<StockItemRef, Double>()
        reservingOrders(savedQuotes, excludingQuoteId).forEach { order ->
            consumptionOf(order, filaments).forEach { (ref, amount) -> result[ref] = (result[ref] ?: 0.0) + amount }
        }
        return result
    }

    /** Situação de [item] com saldo [balance] (item 10): acabando com zero ou menos, baixo abaixo do limite. */
    fun state(item: StockItem, balance: Double): StockState = when {
        balance < EPSILON -> StockState.OUT
        item.lowThreshold != null && balance < item.lowThreshold -> StockState.LOW
        else -> StockState.OK
    }

    /**
     * Os itens controlados que [quote] usa mais do que sobra livre (item 13), na ordem em que aparecem no orçamento. Só
     * avisa: nunca impede. [excludingQuoteId] é o pedido sendo editado, que sai do reservado.
     */
    fun shortages(
        quote: Quote,
        items: List<StockItem>,
        movements: List<StockMovement>,
        savedQuotes: List<SavedQuote>,
        filaments: List<Filament>,
        excludingQuoteId: String? = null,
    ): List<StockShortage> {
        val tracked = items.mapTo(HashSet()) { it.ref }
        val needed = consumptionOf(quote, filaments).filterKeys { it in tracked }
        if (needed.isEmpty()) return emptyList()
        return shortages(needed, balances(movements), reservedByRef(savedQuotes, filaments, excludingQuoteId))
    }

    /**
     * A mesma regra de [shortages], com os números já prontos: quem guarda [balances] e [reserved] entre uma tecla e outra (o
     * Orçamento) não refaz o reservado de todos os pedidos. [needed] é o consumo do orçamento, só dos itens controlados.
     * [excluding] é o consumo do pedido em edição, que sai do reservado pra não contar duas vezes.
     */
    fun shortages(
        needed: Map<StockItemRef, Double>,
        balances: Map<StockItemRef, Double>,
        reserved: Map<StockItemRef, Double>,
        excluding: Map<StockItemRef, Double> = emptyMap(),
    ): List<StockShortage> = needed.mapNotNull { (ref, amount) ->
        // Tirar o próprio pedido pode deixar um resíduo de `Double`: abaixo de [EPSILON] é zero.
        val itemReserved = ((reserved[ref] ?: 0.0) - (excluding[ref] ?: 0.0)).let { if (it < EPSILON) 0.0 else it }
        val available = (balances[ref] ?: 0.0) - itemReserved
        if (amount > available + EPSILON) StockShortage(ref, amount, available, itemReserved) else null
    }

    /** A tela Estoque de uma vez ([StockOverview]). */
    fun overview(
        items: List<StockItem>,
        movements: List<StockMovement>,
        savedQuotes: List<SavedQuote>,
        filaments: List<Filament>,
        consumables: List<Consumable>,
    ): StockOverview {
        val byRef = items.associateBy { it.ref }
        val balances = balances(movements)
        val reserved = reservedByRef(savedQuotes, filaments)
        val tracked = mutableListOf<StockStatus>()
        val untracked = mutableListOf<StockItemRef>()
        for (ref in orderedRefs(filaments, consumables)) {
            val item = byRef[ref]
            if (item == null) {
                untracked += ref
            } else {
                val balance = balances[ref] ?: 0.0
                tracked += StockStatus(item, balance, reserved[ref] ?: 0.0, state(item, balance))
            }
        }
        return StockOverview(tracked, untracked)
    }

    /**
     * A lista de compras (decisão 161). Pra cada item controlado: comprar = max(0, reservado + limite − saldo), com o
     * limite vazio contando zero. À parte, as cores sem controle marcadas "Acabou" que algum pedido Aprovado ou Em
     * impressão usa. Arquivados e excluídos ficam de fora, na ordem de [overview].
     */
    fun shoppingList(
        items: List<StockItem>,
        movements: List<StockMovement>,
        savedQuotes: List<SavedQuote>,
        filaments: List<Filament>,
        consumables: List<Consumable>,
    ): ShoppingList {
        val byRef = items.associateBy { it.ref }
        val balances = balances(movements)
        val reservedByRef = reservedByRef(savedQuotes, filaments)
        // Só a contagem de pedidos por cor esgotada precisa do consumo de cada pedido em separado.
        val consumptions by lazy { reservingOrders(savedQuotes).map { consumptionOf(it, filaments) } }
        val lines = mutableListOf<ShoppingLine>()
        val outOfStock = mutableListOf<OutOfStockColor>()
        val outOfStockRefs = filaments.flatMap { filament -> filament.colors.filter { !it.inStock }.map { StockItemRef.ColorRef(filament.id, it.id) } }.toSet()
        for (ref in orderedRefs(filaments, consumables)) {
            val item = byRef[ref]
            if (item != null) {
                val balance = balances[ref] ?: 0.0
                val reserved = reservedByRef[ref] ?: 0.0
                val amount = reserved + (item.lowThreshold ?: 0.0) - balance
                if (amount > EPSILON) lines += ShoppingLine(ref, amount, reserved, balance, item.lowThreshold)
            } else if (ref is StockItemRef.ColorRef && ref in outOfStockRefs) {
                val count = consumptions.count { (it[ref] ?: 0.0) > 0.0 }
                if (count > 0) outOfStock += OutOfStockColor(ref, count)
            }
        }
        return ShoppingList(lines, outOfStock)
    }

    /**
     * "Média das compras" de [ref] (item 18), na unidade do cadastro (R$/kg na cor, R$/un no insumo): o custo de todas
     * as linhas dele, com o frete rateado, pela quantidade somada. `null` sem compra. Só informação: não muda preço.
     */
    fun averagePurchaseCost(ref: StockItemRef, purchases: List<Purchase>): Double? {
        var cost = 0.0
        var amount = 0.0
        for (purchase in purchases) {
            for (line in purchase.lines) {
                if (line.ref != ref) continue
                cost += purchase.lineCost(line)
                amount += line.amount
            }
        }
        if (amount <= 0) return null
        return cost / amount * ref.catalogFactor
    }

    /** O link da compra mais recente de [ref] que tinha link, pro "Comprar de novo" (item 19). Mesma data: a gravada por último. */
    fun lastPurchaseLink(ref: StockItemRef, purchases: List<Purchase>): String? = purchases
        .withIndex()
        .filter { (_, purchase) -> !purchase.link.isNullOrBlank() && purchase.lines.any { it.ref == ref } }
        .maxWithOrNull(compareBy({ it.value.dateEpochDay }, { it.index }))
        ?.value?.link

    /** Os fornecedores já usados, sem repetir grafia (a primeira usada vale), em ordem alfabética. */
    fun suppliers(purchases: List<Purchase>): List<String> {
        val seen = LinkedHashMap<String, String>()
        purchases.forEach { purchase -> purchase.supplier?.trim()?.takeIf { it.isNotEmpty() }?.let { seen.getOrPut(it.lowercase()) { it } } }
        return seen.values.sortedBy { it.lowercase() }
    }

    /**
     * O fornecedor como vai pro arquivo (mesma regra da categoria do catálogo, decisão 102): sem espaços nas pontas,
     * vazio vira `null`, e com a grafia de um já usado quando só muda maiúscula. [excludingPurchaseId] é a compra sendo
     * editada, pra dar pra corrigir a grafia dela.
     */
    fun normalizedSupplier(input: String?, purchases: List<Purchase>, excludingPurchaseId: String? = null): String? {
        val trimmed = input?.trim()?.ifEmpty { null } ?: return null
        return purchases
            .filter { it.id != excludingPurchaseId }
            .firstNotNullOfOrNull { purchase -> purchase.supplier?.trim()?.takeIf { it.equals(trimmed, ignoreCase = true) } } ?: trimmed
    }

    /** Os itens do cadastro vivo (sem arquivados), na ordem da tela: filamentos por nome e marca, cores por nome, insumos por nome. */
    private fun orderedRefs(filaments: List<Filament>, consumables: List<Consumable>): List<StockItemRef> {
        val colorRefs = filaments
            .filterNot { it.archived }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.brand.orEmpty().lowercase() }))
            .flatMap { filament ->
                filament.colors.sortedBy { it.name.orEmpty().lowercase() }.map { StockItemRef.ColorRef(filament.id, it.id) }
            }
        val consumableRefs = consumables.filterNot { it.archived }.sortedBy { it.name.lowercase() }.map { StockItemRef.ConsumableRef(it.id) }
        return colorRefs + consumableRefs
    }
}
