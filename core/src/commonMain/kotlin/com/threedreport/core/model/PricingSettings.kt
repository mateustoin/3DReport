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
 */
@Serializable
data class PricingSettings(
    val energyPricePerKwh: Double,
    val failureRate: Double,
    val finishingRate: Double,
    val administrativeCost: Double = 0.0,
    val profitMargin: Double,
) {
    init {
        require(energyPricePerKwh >= 0) { "energyPricePerKwh não pode ser negativo" }
        require(failureRate >= 0) { "failureRate não pode ser negativo" }
        require(finishingRate >= 0) { "finishingRate não pode ser negativo" }
        require(administrativeCost >= 0) { "administrativeCost não pode ser negativo" }
        require(profitMargin >= 0) { "profitMargin não pode ser negativo" }
    }
}
