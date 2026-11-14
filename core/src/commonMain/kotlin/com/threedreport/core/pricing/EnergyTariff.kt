package com.threedreport.core.pricing

/**
 * Ajuda pra chegar ao preço do kWh (decisão 126), sem mudar a conta: o resultado vai pro
 * [com.threedreport.core.model.PricingSettings.energyPricePerKwh] como qualquer valor digitado. Nenhum
 * valor da ANEEL vem embutido, porque tarifa e bandeira mudam, e um número velho erraria o preço em
 * silêncio.
 */
object EnergyTariff {

    /**
     * O kWh de verdade, pela conta de luz: o valor total dividido pelo consumo do mês. Já inclui impostos,
     * bandeira e taxas, então é o jeito mais simples e mais exato. `null` sem consumo.
     */
    fun fromBill(billTotal: Double, consumedKwh: Double): Double? {
        require(billTotal >= 0) { "billTotal não pode ser negativo: $billTotal" }
        require(consumedKwh >= 0) { "consumedKwh não pode ser negativo: $consumedKwh" }
        return if (consumedKwh > 0) billTotal / consumedKwh else null
    }

    /** A tarifa com o acréscimo da bandeira do mês (amarela, vermelha), os dois em R$ por kWh. */
    fun withFlag(tariffPerKwh: Double, flagSurchargePerKwh: Double): Double {
        require(tariffPerKwh >= 0) { "tariffPerKwh não pode ser negativo: $tariffPerKwh" }
        require(flagSurchargePerKwh >= 0) { "flagSurchargePerKwh não pode ser negativo: $flagSurchargePerKwh" }
        return tariffPerKwh + flagSurchargePerKwh
    }

    /**
     * Um período de uma tarifa por horário (tarifa branca): o preço do kWh nele e quanto das horas de
     * impressão caem nele, em qualquer unidade (percentual ou horas), porque o resultado é uma média.
     */
    data class Period(val pricePerKwh: Double, val share: Double) {
        init {
            require(pricePerKwh >= 0) { "pricePerKwh não pode ser negativo: $pricePerKwh" }
            require(share >= 0) { "share não pode ser negativo: $share" }
        }
    }

    /**
     * O kWh médio de quem imprime em horários com preços diferentes (tarifa branca: fora de ponta,
     * intermediário e ponta), pesado por quanto das horas de impressão cai em cada um. `null` quando
     * nenhum período tem horas.
     */
    fun weightedAverage(periods: List<Period>): Double? {
        val totalShare = periods.sumOf { it.share }
        return if (totalShare > 0) periods.sumOf { it.pricePerKwh * it.share } / totalShare else null
    }
}
