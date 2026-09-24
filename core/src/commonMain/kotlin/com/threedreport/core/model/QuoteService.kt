package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Serviço cobrado num orçamento: retrato do nome, do valor e da forma de
 * cobrança no momento em que foi salvo (ver [SavedQuote]). Mudar depois o
 * [Service] do catálogo não mexe aqui.
 *
 * Tem o mesmo formato JSON que o `Service` salvo no histórico por versões
 * anteriores (`id`, `name`, `price`), então orçamentos antigos abrem sem
 * migração e caem em [chargedPerOrder] `false`, que é como foram calculados.
 *
 * @property id id do [Service] de origem no catálogo.
 * @property price valor cobrado, em R$: por peça ou pelo pedido inteiro,
 *   conforme [chargedPerOrder].
 * @property chargedPerOrder `true` cobra [price] uma vez pelo pedido;
 *   `false` multiplica pela quantidade.
 */
@Serializable
data class QuoteService(
    val id: String,
    val name: String,
    val price: Double,
    val chargedPerOrder: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(price >= 0) { "price não pode ser negativo" }
    }

    /** Quanto este serviço soma ao total de um pedido com [quantity] peças. */
    fun total(quantity: Int): Double = if (chargedPerOrder) price else price * quantity
}
