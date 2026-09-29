package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Insumo que entra na peça e custa dinheiro, além do filamento (decisão 122): argola de chaveiro, ímã,
 * parafuso, tinta, caixa, saquinho. Salvo em catálogo, como [Service], mas é o contrário dele: o serviço
 * é o que se **cobra** do cliente, o insumo é o que **custa** pra você.
 *
 * Entra no custo de produção e passa pela margem, como o material. Fica fora da reserva de falha, porque
 * argola e caixa entram depois da impressão: uma peça que falha não gasta insumo.
 *
 * @property unitCost quanto custa uma unidade, em R$.
 * @property chargedPerOrder padrão ao usar num orçamento: `false` vezes a quantidade de peças (uma argola
 *   por chaveiro), `true` uma vez pelo pedido (uma caixa pra entrega inteira). O orçamento pode trocar.
 */
@Serializable
data class Consumable(
    override val id: String,
    val name: String,
    val unitCost: Double,
    val chargedPerOrder: Boolean = false,
    override val archived: Boolean = false,
) : Archivable<Consumable> {
    override fun withArchived(archived: Boolean): Consumable = copy(archived = archived)

    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(unitCost >= 0) { "unitCost não pode ser negativo: $unitCost" }
    }
}

/**
 * Insumo usado num orçamento: retrato do nome e do custo no momento do cálculo, com a quantidade. Mudar
 * depois o [Consumable] do catálogo não mexe no pedido salvo (o produto do catálogo é recalculado pelo
 * `ProductRepricer` com o custo de hoje).
 *
 * @property quantity unidades usadas: por peça, ou no pedido inteiro quando [chargedPerOrder].
 */
@Serializable
data class QuotedConsumable(
    val id: String,
    val name: String,
    val unitCost: Double,
    val quantity: Double,
    val chargedPerOrder: Boolean,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(unitCost >= 0) { "unitCost não pode ser negativo: $unitCost" }
        require(quantity > 0) { "quantity deve ser positiva: $quantity" }
    }

    /** Custo deste insumo num pedido com [orderQuantity] peças. */
    fun total(orderQuantity: Int): Double = unitCost * quantity * (if (chargedPerOrder) 1 else orderQuantity)

    companion object {
        /** O [consumable] do catálogo usado [quantity] vezes, com o custo de hoje. */
        fun of(consumable: Consumable, quantity: Double, chargedPerOrder: Boolean = consumable.chargedPerOrder) =
            QuotedConsumable(consumable.id, consumable.name, consumable.unitCost, quantity, chargedPerOrder)
    }
}
