package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Serviço opcional oferecido junto com a impressão (ex.: pintura, lixamento,
 * acabamento). Salvo em catálogo — cada criador tem os seus, com o preço que
 * quiser cobrar; um orçamento escolhe quais se aplicam àquela peça.
 *
 * Diferente de [Filament]/[PrinterProfile], não tem custo próprio modelado
 * — [price] já é o valor cobrado do cliente, então soma direto no valor de
 * venda do orçamento, sem passar pela margem de lucro (ver
 * `pricing/PricingCalculator`, que não conhece serviços).
 *
 * @property id identificador único, atribuído por quem cria o serviço (UI).
 * @property name nome livre para identificação (ex.: "Pintura", "Lixamento").
 * @property price valor cobrado do cliente por este serviço, em R$.
 */
@Serializable
data class Service(
    val id: String,
    val name: String,
    val price: Double,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(price >= 0) { "price não pode ser negativo" }
    }
}
