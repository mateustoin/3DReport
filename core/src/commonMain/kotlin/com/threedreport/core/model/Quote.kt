package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Detalhamento dos custos de produção de uma peça, em R$.
 *
 * Os valores não são arredondados; arredonde apenas na exibição.
 */
@Serializable
data class CostBreakdown(
    val material: Double,
    val energy: Double,
    val maintenance: Double,
    val failures: Double,
    val finishing: Double,
    val investmentReturn: Double,
    val administrative: Double,
) {
    /** Soma de todos os custos: o valor de produção. */
    val total: Double
        get() = material + energy + maintenance + failures + finishing + investmentReturn + administrative
}

/**
 * Resultado de um orçamento.
 *
 * @property job peça orçada.
 * @property filamentWeightGrams massa estimada de filamento, em gramas.
 * @property costs detalhamento dos custos.
 * @property productionCost valor de produção (= [CostBreakdown.total]).
 * @property salePrice valor de venda (= produção · (1 + margem)).
 */
@Serializable
data class Quote(
    val job: PrintJob,
    val filamentWeightGrams: Double,
    val costs: CostBreakdown,
    val productionCost: Double,
    val salePrice: Double,
) {
    /** Lucro bruto: venda − produção. */
    val profit: Double
        get() = salePrice - productionCost
}
