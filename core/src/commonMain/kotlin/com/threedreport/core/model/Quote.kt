package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Detalhamento dos custos de produção, em R$.
 *
 * Os valores são do **pedido inteiro** (já multiplicados pela quantidade do
 * orçamento, ver [Quote]), e não de uma unidade. Não são arredondados;
 * arredonde apenas na exibição.
 *
 * @property labor seu tempo de trabalho: o de cada peça (ver
 *   [PrintJob.laborMinutes]) multiplicado pela quantidade, mais o preparo
 *   cobrado uma vez pelo pedido (ver [Quote.setupMinutes]). `0.0` em orçamentos
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
 * **Todos os valores em dinheiro e de massa aqui são do pedido inteiro**, já
 * multiplicados por [quantity] — é o que o cliente paga e o que sai do seu
 * carretel. O preço de uma unidade é [unitSalePrice]. O que continua sendo
 * "de uma peça só" é o [job] (comprimento, tempo e minutos de trabalho por
 * unidade), porque é assim que o fatiador informa.
 *
 * @property job peça orçada (dados de **uma** unidade).
 * @property quantity quantas peças iguais este orçamento cobre.
 * @property setupMinutes minutos de preparo cobrados **uma vez** pelo pedido
 *   inteiro (preparar o arquivo, fatiar, montar a mesa), independente de
 *   [quantity]. É o que faz o preço por unidade cair conforme a quantidade
 *   sobe, sem precisar inventar um desconto.
 * @property filamentWeightGrams massa estimada de filamento do pedido
 *   inteiro, em gramas.
 * @property costs detalhamento dos custos do pedido inteiro.
 * @property productionCost valor de produção do pedido (= [CostBreakdown.total]).
 * @property salePrice valor de venda (= produção · (1 + margem), já ajustado
 *   pra compensar [marketplaceFeeRate] quando aplicável — é o preço de fato
 *   cobrado do cliente, o marketplace não aparece pra ele).
 * @property marketplaceFeeRate taxa do canal de venda escolhido (ver
 *   [SalesChannel]) já embutida em [salePrice] (`0.0` na venda direta). O
 *   nome do campo é o antigo de propósito: renomear faria orçamentos já
 *   salvos perderem a taxa e passarem a exibir um lucro maior do que o real.
 * @property channelName nome do canal usado, guardado à parte porque o
 *   catálogo pode mudar depois (mesmo motivo de [printerName]). `null` em
 *   venda direta e em orçamentos salvos antes deste campo existir.
 * @property taxRate imposto sobre a venda já embutido em [salePrice]
 *   (ver [PricingSettings.taxRate]).
 * @property printerId/[printerName] identificam a impressora usada no
 *   cálculo (nome guardado à parte porque o perfil pode ser editado/
 *   excluído do catálogo depois) — uso interno, principalmente pra
 *   conseguir reabrir um orçamento salvo pra edição já com a mesma
 *   impressora selecionada. `null` em orçamentos salvos antes desse campo
 *   existir.
 * @property tableSalePrice valor de venda que a margem configurada daria,
 *   guardado **só quando o preço foi negociado** com o cliente (aí
 *   [salePrice] é o preço fechado). `null` quando não houve negociação e em
 *   orçamentos salvos antes deste campo existir. Uso interno: nunca entra
 *   em nenhum export.
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
    val quantity: Int = 1,
    val setupMinutes: Double = 0.0,
    val channelName: String? = null,
    val taxRate: Double = 0.0,
    val tableSalePrice: Double? = null,
) {
    init {
        require(quantity >= 1) { "quantity deve ser pelo menos 1: $quantity" }
        require(setupMinutes >= 0) { "setupMinutes não pode ser negativo: $setupMinutes" }
    }

    /** Tudo que é descontado da venda antes de o dinheiro chegar em você. */
    val totalDeductionRate: Double
        get() = marketplaceFeeRate + taxRate

    /**
     * Lucro líquido real do pedido: o que sobra depois de canal e imposto
     * descontarem suas partes de [salePrice], menos a produção. Sem
     * deduções, é só venda − produção.
     */
    val profit: Double
        get() = salePrice * (1 - totalDeductionRate) - productionCost

    /** Preço de uma unidade: [salePrice] dividido por [quantity]. */
    val unitSalePrice: Double
        get() = salePrice / quantity

    /**
     * Menor valor de venda que ainda não dá prejuízo: cobre o custo de
     * produção depois de canal e imposto levarem a parte deles. Vender
     * exatamente por isso significa trabalhar de graça; abaixo disso, você
     * paga pra imprimir. É a referência pra negociar sem ter que refazer a
     * conta de cabeça.
     */
    val breakEvenSalePrice: Double
        get() = productionCost / (1 - totalDeductionRate)

    /** Se o preço foi fechado com o cliente em vez de vir da margem (ver [tableSalePrice]). */
    val isNegotiated: Boolean
        get() = tableSalePrice != null

    /**
     * Quanto o preço fechado ficou abaixo do de tabela: positivo quando houve
     * desconto, negativo quando o cliente pagou acima da tabela, zero sem
     * negociação. Somar isso num período dá o desconto líquido concedido.
     */
    val negotiatedDiscount: Double
        get() = (tableSalePrice ?: salePrice) - salePrice

    /** Lucro como fração do custo de produção, que é a margem de fato obtida neste orçamento. */
    val actualProfitMargin: Double
        get() = if (productionCost > 0) profit / productionCost else 0.0
}
