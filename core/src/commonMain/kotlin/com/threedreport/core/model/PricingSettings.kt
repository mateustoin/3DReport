package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Parâmetros de custo do negócio, compartilhados entre todos os orçamentos e
 * impressoras (o que é específico de cada impressora fica em [PrinterProfile]).
 *
 * Percentuais são frações decimais: 10% = `0.10`, 100% = `1.0`.
 *
 * @property energyPricePerKwh preço do kWh, em R$.
 * @property failureRate percentual sobre o custo de material reservado para falhas de impressão.
 * @property finishingRate percentual sobre o custo de material referente a acabamento.
 * @property administrativeCost custo fixo administrativo por orçamento (ex.: modelagem 3D), em R$.
 * @property profitMargin margem de lucro aplicada sobre o custo de produção.
 * @property marketplaceFeeRate percentual que um marketplace (ex.: Shopee) desconta da venda,
 *   quando o orçamento marcar que a peça será vendida por lá. Diferente de um serviço extra: não
 *   é somado ao total cobrado do cliente, é descontado do que o criador recebe — por isso o valor
 *   de venda aumenta o suficiente para que a margem de lucro real não mude (ver
 *   `pricing/PricingCalculator`).
 */
@Serializable
data class PricingSettings(
    val energyPricePerKwh: Double,
    val failureRate: Double,
    val finishingRate: Double,
    val administrativeCost: Double = 0.0,
    val profitMargin: Double,
    val marketplaceFeeRate: Double = 0.0,
) {
    init {
        require(energyPricePerKwh >= 0) { "energyPricePerKwh não pode ser negativo" }
        require(failureRate >= 0) { "failureRate não pode ser negativo" }
        require(finishingRate >= 0) { "finishingRate não pode ser negativo" }
        require(administrativeCost >= 0) { "administrativeCost não pode ser negativo" }
        require(profitMargin >= 0) { "profitMargin não pode ser negativo" }
        require(marketplaceFeeRate >= 0 && marketplaceFeeRate < 1) {
            "marketplaceFeeRate deve estar entre 0 (inclusive) e 1 (exclusive)"
        }
    }
}
