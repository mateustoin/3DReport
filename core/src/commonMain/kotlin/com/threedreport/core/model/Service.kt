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
 * @property archived arquivado (decisão 115): some das escolhas de um orçamento novo, mas continua no
 *   cadastro pra quem já usou. Pedidos reabertos e produtos do catálogo continuam achando ele.
 * @property laborMinutes quanto do seu tempo o serviço leva (decisão 123), por peça ou pelo pedido,
 *   conforme [chargedPerOrder]. Sugere o valor quando não há [suggestedPrice] (minutos vezes a sua hora)
 *   e faz o lucro contar o serviço pelo que ele rende acima da sua hora. `null` (padrão) deixa o serviço
 *   como sempre foi: repasse, fora do lucro.
 */
@Serializable
data class Service(
    override val id: String,
    val name: String,
    val suggestedPrice: Double? = null,
    val chargedPerOrder: Boolean = false,
    override val archived: Boolean = false,
    val laborMinutes: Double? = null,
) : Archivable<Service> {
    override fun withArchived(archived: Boolean): Service = copy(archived = archived)

    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(suggestedPrice == null || suggestedPrice >= 0) { "suggestedPrice não pode ser negativo" }
        require(laborMinutes == null || laborMinutes >= 0) { "laborMinutes não pode ser negativo" }
    }

    /**
     * Valor que o orçamento sugere ao marcar este serviço: o [suggestedPrice] do cadastro, ou, sem ele, os
     * [laborMinutes] pagos pela sua hora de trabalho ([laborRatePerHour]). `null` quando não há nenhum dos dois.
     */
    fun suggestedPriceFor(laborRatePerHour: Double): Double? =
        suggestedPrice ?: laborMinutes?.takeIf { it > 0 && laborRatePerHour > 0 }?.let { it / 60.0 * laborRatePerHour }
}
