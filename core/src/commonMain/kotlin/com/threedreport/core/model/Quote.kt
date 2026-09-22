package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Detalhamento dos custos de produção de uma peça, em R$.
 *
 * Os valores não são arredondados; arredonde apenas na exibição.
 *
 * @property labor seu tempo de trabalho na peça (ver [PrintJob.laborMinutes]). `0.0` em orçamentos
 *   salvos antes deste campo existir e para quem não configurou taxa de mão de obra.
 * @property fixedCost parcela do custo fixo mensal do negócio que esta peça paga, proporcional às
 *   horas de impressão (ver [PricingSettings.fixedCostPerHour]).
 * @property finishing acabamento no modelo antigo (percentual sobre o material); fica `0.0` quando
 *   há taxa de mão de obra configurada, porque aí o acabamento entra em [labor].
 * @property failures reserva de falha, que incide sobre todos os outros custos menos
 *   [administrative] (ver [PricingSettings.failureRate]).
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
    val labor: Double = 0.0,
    val fixedCost: Double = 0.0,
) {
    /** Soma de todos os custos: o valor de produção. */
    val total: Double
        get() = material + energy + maintenance + failures + finishing + investmentReturn +
            administrative + labor + fixedCost
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
