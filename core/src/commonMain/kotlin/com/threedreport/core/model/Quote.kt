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
 * @property salePrice valor de venda (= produção · (1 + margem), já ajustado
 *   pra compensar [marketplaceFeeRate] quando aplicável — é o preço de fato
 *   cobrado do cliente, o marketplace não aparece pra ele).
 * @property marketplaceFeeRate percentual do marketplace já embutido em
 *   [salePrice] para este orçamento (`0.0` se não vendido por marketplace).
 * @property printerId/[printerName] identificam a impressora usada no
 *   cálculo (nome guardado à parte porque o perfil pode ser editado/
 *   excluído do catálogo depois) — uso interno, principalmente pra
 *   conseguir reabrir um orçamento salvo pra edição já com a mesma
 *   impressora selecionada. `null` em orçamentos salvos antes desse campo
 *   existir.
 */
@Serializable
data class Quote(
    val job: PrintJob,
    val filamentWeightGrams: Double,
    val costs: CostBreakdown,
    val productionCost: Double,
    val salePrice: Double,
    val marketplaceFeeRate: Double = 0.0,
    val printerId: String? = null,
    val printerName: String? = null,
) {
    /**
     * Lucro líquido real: o que sobra depois do marketplace descontar sua
     * parte de [salePrice] (quando [marketplaceFeeRate] > 0), menos a
     * produção. Sem marketplace, é só venda − produção.
     */
    val profit: Double
        get() = salePrice * (1 - marketplaceFeeRate) - productionCost
}
