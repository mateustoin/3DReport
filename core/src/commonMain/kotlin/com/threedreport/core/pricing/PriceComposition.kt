package com.threedreport.core.pricing

import com.threedreport.core.model.Quote

/**
 * O preço que o cliente paga ([Quote.customerTotal]) dividido nos quatro grupos da barra do painel de
 * resultado, na ordem da barra (decisão 130). Os quatro somam o total ([total]).
 *
 * @property materialAndMachine a produção menos o trabalho e o acabamento: material, energia,
 *   manutenção, retorno da máquina, custo fixo, administrativo, insumos e a reserva de falha.
 * @property laborAndServices seu trabalho, o acabamento e os serviços. De um serviço com tempo informado
 *   só entra o que vale a sua hora; o que ele rende acima disso está em [profit] (decisão 123).
 * @property feesAndTaxes o que o canal leva (percentual e fixo), o imposto e o frete, cobrado do cliente
 *   ou pago por você no frete grátis.
 * @property profit o lucro do pedido ([Quote.profit]). É o único grupo que pode ser negativo (preço
 *   fechado abaixo do custo): aí os outros três passam do total.
 */
data class PriceComposition(
    val materialAndMachine: Double,
    val laborAndServices: Double,
    val feesAndTaxes: Double,
    val profit: Double,
) {
    /** A soma dos quatro grupos: o que o cliente paga. */
    val total: Double
        get() = materialAndMachine + laborAndServices + feesAndTaxes + profit
}

/**
 * Os quatro grupos do preço deste pedido (ver [PriceComposition]).
 *
 * O [Quote] guarda serviços e frete juntos em [Quote.extrasTotal]; a parte que é frete vem em
 * [chargedShipping] (o frete cobrado do cliente, zero no frete grátis, ver
 * [com.threedreport.core.model.SavedQuote.chargedShipping] e [OrderTerms.chargedShipping]). O resto dos
 * extras são os serviços.
 *
 * A soma fecha porque [Quote.profit] é o total menos exatamente o que vai nos outros três grupos. Nunca
 * lança: um [chargedShipping] fora de `0..extrasTotal` (registro antigo inconsistente) é limitado a esse
 * intervalo, e a soma continua fechando.
 */
fun Quote.priceComposition(chargedShipping: Double): PriceComposition {
    // Um registro antigo ou inconsistente pode guardar um frete maior que os extras (ou negativo): o painel
    // não pode cair por isso. O frete é limitado ao que cabe nos extras, e os grupos continuam somando o total.
    val shipping = if (chargedShipping.isNaN()) 0.0 else chargedShipping.coerceIn(0.0, extrasTotal)
    val servicesTotal = extrasTotal - shipping
    return PriceComposition(
        materialAndMachine = productionCost - costs.labor - costs.finishing,
        laborAndServices = costs.labor + costs.finishing + servicesTotal - serviceProfit,
        feesAndTaxes = channelFeeAmount + taxAmount + shipping + absorbedShippingCost,
        profit = profit,
    )
}
