package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Serviço cobrado num orçamento: retrato do nome, do valor e da forma de
 * cobrança no momento em que foi salvo (ver [SavedQuote]). Mudar depois o
 * [Service] do catálogo não mexe aqui.
 *
 * @property id id do [Service] de origem no catálogo.
 * @property price valor cobrado, em R$: por peça ou pelo pedido inteiro,
 *   conforme [chargedPerOrder].
 * @property chargedPerOrder `true` cobra [price] uma vez pelo pedido;
 *   `false` multiplica pela quantidade.
 * @property laborMinutes seu tempo no serviço, por peça ou pelo pedido como [price] (decisão 123).
 *   Zero (padrão) deixa o serviço fora do lucro, como repasse.
 */
@Serializable
data class QuoteService(
    val id: String,
    val name: String,
    val price: Double,
    val chargedPerOrder: Boolean,
    val laborMinutes: Double = 0.0,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(price >= 0) { "price não pode ser negativo" }
        require(laborMinutes >= 0) { "laborMinutes não pode ser negativo" }
    }

    /** Quanto este serviço soma ao total de um pedido com [quantity] peças. */
    fun total(quantity: Int): Double = if (chargedPerOrder) price else price * quantity

    /** Seu tempo neste serviço num pedido com [quantity] peças, em minutos. */
    fun totalLaborMinutes(quantity: Int): Double = if (chargedPerOrder) laborMinutes else laborMinutes * quantity

    /** Se o serviço tem tempo informado, e por isso conta no lucro (ver [laborMinutes]). */
    val isTimed: Boolean
        get() = laborMinutes > 0
}
