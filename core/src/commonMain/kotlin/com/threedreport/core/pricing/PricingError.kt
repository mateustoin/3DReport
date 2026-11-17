package com.threedreport.core.pricing

/**
 * Por que um preço não pôde ser calculado, nos casos que a tela precisa explicar a quem usa o app
 * (decisão 130). O `core` não escreve frase pra tela: cada caso é um tipo, com os números que a frase
 * pode citar, e quem mostra (`composeApp`, `web`) escolhe o texto pelo tipo.
 *
 * Herda de [IllegalArgumentException] pra quem já tratava a entrada recusada continuar tratando. A
 * [message] é pra quem programa, nunca pra tela. Os `require` de proteção interna (valor negativo,
 * quantidade zero) continuam como [IllegalArgumentException] comum: a tela valida antes.
 */
sealed class PricingError(message: String) : IllegalArgumentException(message) {

    /**
     * A taxa do canal somada ao imposto chega a 100% do valor de venda: nenhum preço deixa dinheiro na
     * sua mão.
     *
     * @property channelFeeRate a taxa do canal envolvida, em fração. Com faixas de preço, a menor delas:
     *   nem a faixa mais barata deixa sobrar alguma coisa.
     * @property taxRate o imposto sobre a venda, em fração.
     */
    class DeductionsReachFullPrice(val channelFeeRate: Double, val taxRate: Double) :
        PricingError("channel fee ($channelFeeRate) plus tax ($taxRate) reaches 100% of the sale price")
}
