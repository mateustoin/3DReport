package com.threedreport.core.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Uma linha de [Purchase]: o item, quanto veio e quanto custou a linha.
 *
 * @property amount quantidade, em gramas (cor de filamento) ou unidades (insumo), como o saldo ([StockItemRef.unit]).
 * @property price preço pago pela linha inteira, sem o frete.
 */
@Serializable
data class PurchaseLine(
    val ref: StockItemRef,
    val amount: Double,
    val price: Double,
) {
    init {
        require(amount > 0) { "amount deve ser positivo: $amount" }
        require(price >= 0) { "price não pode ser negativo: $price" }
    }
}

/**
 * Uma compra de material (decisão 160): uma ou mais linhas, com o frete rateado entre elas pelo valor. Fica registrada
 * mesmo se o item não tem controle de saldo, porque é dinheiro (a Leva 13 usa). Nunca muda o preço de nada sozinha: o
 * preço do cadastro só muda pelo [catalogPriceUpdates] confirmado na tela.
 *
 * @property dateEpochDay data da compra, em dias desde 01/01/1970 (mesma unidade do prazo de entrega).
 * @property supplier fornecedor, texto livre com sugestão dos já usados ([com.threedreport.core.report.StockReport.suppliers]).
 * @property link onde comprar de novo.
 * @property shipping frete pago, em R$. Zero = sem frete.
 * @property lines o que veio; nunca vazia.
 */
@Serializable
data class Purchase(
    val id: String,
    val dateEpochDay: Long,
    val supplier: String? = null,
    val link: String? = null,
    val shipping: Double = 0.0,
    val note: String? = null,
    val lines: List<PurchaseLine>,
) {
    init {
        require(lines.isNotEmpty()) { "uma compra precisa de pelo menos uma linha" }
        require(shipping >= 0) { "shipping não pode ser negativo: $shipping" }
    }

    /** Soma dos preços das linhas, sem o frete. */
    val linesTotal: Double
        get() = lines.sumOf { it.price }

    /** Total pago: linhas mais frete. */
    val total: Double
        get() = linesTotal + shipping

    /**
     * A parte do frete que cabe a [line]: proporcional ao preço dela. Se todas as linhas custaram zero (brinde, troca),
     * o frete é dividido igual entre elas.
     */
    fun shippingShare(line: PurchaseLine): Double {
        val sum = linesTotal
        return if (sum > 0.0) shipping * line.price / sum else shipping / lines.size
    }

    /** Custo da linha: o preço dela mais a parte do frete ([shippingShare]). */
    fun lineCost(line: PurchaseLine): Double = line.price + shippingShare(line)

    /** Custo de uma unidade do saldo (um grama ou uma unidade de insumo) nesta linha. */
    fun unitCost(line: PurchaseLine): Double = lineCost(line) / line.amount

    /**
     * Custo na unidade do cadastro: por kg na cor de filamento (comparável a [Filament.pricePerKg]) e por unidade no
     * insumo (comparável a [Consumable.unitCost]). É o que o formulário mostra ao lado de cada linha.
     */
    fun catalogUnitCost(line: PurchaseLine): Double = unitCost(line) * line.ref.catalogFactor

    /** Quanto veio de cada item, somando as linhas repetidas. */
    fun amountByRef(): Map<StockItemRef, Double> {
        val result = LinkedHashMap<StockItemRef, Double>()
        lines.forEach { result[it.ref] = (result[it.ref] ?: 0.0) + it.amount }
        return result
    }

    /**
     * Custo por kg de cada filamento da compra (item 17): o custo somado das linhas dele, de todas as cores, dividido
     * pelos kg somados.
     */
    fun costPerKgByFilament(): Map<String, Double> = costByCatalogId { (it as? StockItemRef.ColorRef)?.filamentId }

    /** Custo por unidade de cada insumo da compra: o custo somado das linhas dele pelas unidades somadas. */
    fun unitCostByConsumable(): Map<String, Double> = costByCatalogId { (it as? StockItemRef.ConsumableRef)?.consumableId }

    private fun costByCatalogId(idOf: (StockItemRef) -> String?): Map<String, Double> {
        val cost = LinkedHashMap<String, Double>()
        val amount = LinkedHashMap<String, Double>()
        for (line in lines) {
            val id = idOf(line.ref) ?: continue
            cost[id] = (cost[id] ?: 0.0) + lineCost(line)
            amount[id] = (amount[id] ?: 0.0) + line.amount / line.ref.catalogFactor
        }
        return cost.mapValues { (id, total) -> total / amount.getValue(id) }
    }

    /**
     * Os preços do cadastro que esta compra sugere atualizar (item 17): cada filamento e cada insumo da compra cujo
     * custo novo difere do cadastrado em mais de meio centavo. Item que não está mais no cadastro fica de fora. Só
     * lista: quem aplica é a tela, nos que a pessoa deixou marcados ([CatalogPriceUpdate.applyTo]).
     */
    fun catalogPriceUpdates(filaments: List<Filament>, consumables: List<Consumable>): List<CatalogPriceUpdate> {
        val forFilaments = costPerKgByFilament().mapNotNull { (id, cost) ->
            val filament = filaments.find { it.id == id } ?: return@mapNotNull null
            if (differs(cost, filament.pricePerKg)) CatalogPriceUpdate.FilamentPrice(id, filament.pricePerKg, cost) else null
        }
        val forConsumables = unitCostByConsumable().mapNotNull { (id, cost) ->
            val consumable = consumables.find { it.id == id } ?: return@mapNotNull null
            if (differs(cost, consumable.unitCost)) CatalogPriceUpdate.ConsumableCost(id, consumable.unitCost, cost) else null
        }
        return forFilaments + forConsumables
    }

    private fun differs(a: Double, b: Double) = abs(a - b) > PRICE_TOLERANCE
}

/** Diferença de preço abaixo da qual a compra não sugere mudar o cadastro: meio centavo. */
private const val PRICE_TOLERANCE = 0.005

/** Quantas unidades do saldo cabem numa unidade do cadastro: 1000 g num kg, 1 unidade numa unidade. */
private val StockItemRef.catalogFactor: Double
    get() = when (this) {
        is StockItemRef.ColorRef -> GRAMS_PER_KG
        is StockItemRef.ConsumableRef -> 1.0
    }

private const val GRAMS_PER_KG = 1000.0

/** Um preço do cadastro que uma compra sugere trocar ([Purchase.catalogPriceUpdates]). */
sealed interface CatalogPriceUpdate {
    /** O preço por kg do filamento [filamentId], de [currentPricePerKg] pra [newPricePerKg]. */
    data class FilamentPrice(val filamentId: String, val currentPricePerKg: Double, val newPricePerKg: Double) : CatalogPriceUpdate {
        /** O filamento com o preço novo, e nada mais mudado. */
        fun applyTo(filament: Filament): Filament = filament.copy(pricePerKg = newPricePerKg)
    }

    /** O custo por unidade do insumo [consumableId], de [currentUnitCost] pra [newUnitCost]. */
    data class ConsumableCost(val consumableId: String, val currentUnitCost: Double, val newUnitCost: Double) : CatalogPriceUpdate {
        /** O insumo com o custo novo, e nada mais mudado. */
        fun applyTo(consumable: Consumable): Consumable = consumable.copy(unitCost = newUnitCost)
    }
}
