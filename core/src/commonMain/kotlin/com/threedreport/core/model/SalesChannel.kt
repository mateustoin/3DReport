package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Por onde a venda acontece e quanto isso custa de percentual sobre o valor
 * recebido: um marketplace (Shopee, Mercado Livre), uma forma de recebimento
 * (cartão, link de pagamento) ou a venda direta sem taxa nenhuma (Pix,
 * dinheiro). Salvo em catálogo, igual a [Service] — cada criador cadastra os
 * canais que usa, e cada orçamento escolhe um.
 *
 * **Marketplace e forma de pagamento são o mesmo campo de propósito.** Somar
 * "Shopee 20%" com "cartão 4%" cobraria em dobro, porque a taxa do
 * marketplace já embute o processamento do pagamento. Quem precisa dos dois
 * ao mesmo tempo (ex.: loja própria com maquininha) cadastra um canal com a
 * soma que de fato paga.
 *
 * Diferente de um [Service], que é somado ao que o cliente paga: a taxa do
 * canal é **descontada** do que você recebe, então o valor de venda sobe o
 * suficiente pra sua margem real não mudar (ver `pricing/PricingCalculator`).
 *
 * @property id identificador único, atribuído por quem cria o canal (UI).
 * @property name nome livre (ex.: "Shopee", "Cartão", "Pix").
 * @property feeRate fração descontada da venda (0,20 = 20%).
 * @property archived arquivado (decisão 115): some das escolhas de um orçamento novo, mas continua no
 *   cadastro pra quem já usou. Pedidos reabertos e produtos do catálogo continuam achando ele.
 */
@Serializable
data class SalesChannel(
    val id: String,
    val name: String,
    val feeRate: Double,
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(feeRate >= 0 && feeRate < 1) { "feeRate deve estar entre 0 (inclusive) e 1 (exclusive): $feeRate" }
    }
}
