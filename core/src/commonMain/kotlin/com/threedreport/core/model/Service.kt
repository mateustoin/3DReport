package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Serviço opcional oferecido junto com a impressão (ex.: pintura, lixamento,
 * entrega). Salvo em catálogo, cada criador tem os seus; um orçamento escolhe
 * quais se aplicam àquele pedido e **digita o valor ali**, porque pintar uma
 * peça grande não custa o mesmo que pintar um chaveiro. O que o orçamento
 * cobrou fica congelado em [QuoteService].
 *
 * Não tem custo próprio modelado: o valor já é o que se cobra do cliente,
 * então soma direto no total do pedido, sem passar pela margem de lucro (ver
 * `pricing/PricingCalculator`, que não conhece serviços).
 *
 * @property id identificador único, atribuído por quem cria o serviço (UI).
 * @property name nome livre para identificação (ex.: "Pintura", "Entrega").
 * @property suggestedPrice valor sugerido, em R$: só preenche o campo quando o
 *   serviço é marcado num orçamento, e pode ser mudado ali. `null` quando o
 *   valor muda a cada pedido.
 * @property chargedPerOrder padrão de cobrança ao marcar este serviço:
 *   `false` multiplica pela quantidade (pintura, lixamento: trabalho peça a
 *   peça), `true` cobra uma vez pelo pedido (entrega, modelagem). O
 *   orçamento pode trocar.
 */
@Serializable
data class Service(
    val id: String,
    val name: String,
    val suggestedPrice: Double? = null,
    val chargedPerOrder: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(suggestedPrice == null || suggestedPrice >= 0) { "suggestedPrice não pode ser negativo" }
    }
}
