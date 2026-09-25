package com.threedreport.core.model

/**
 * Agregação de uma lista de [SavedQuote] (ver [com.threedreport.core.report.QuoteReport]),
 * usada no Dashboard.
 *
 * **Venda é o que o cliente fechou** ([OrderStatus.isSold], decisão 95): todos os números abaixo
 * são só dos pedidos vendidos, menos os `open*`, que são dos orçamentos ainda em [OrderStatus.ORCADO].
 *
 * @property quoteCount quantos pedidos vendidos entraram na soma.
 * @property totalSalePrice o que foi vendido: peça + serviços de cada pedido, **sem o frete**, que é
 *   repasse pra transportadora e não faturamento seu.
 * @property totalProfit soma de [Quote.profit].
 * @property mostUsedFilamentName nome do filamento mais usado no período, ou
 *   `null` se não houver nenhuma venda.
 * @property mostUsedFilamentCount quantos pedidos usaram [mostUsedFilamentName].
 * @property negotiatedCount quantos pedidos tiveram o preço fechado com o cliente
 *   (ver [Quote.isNegotiated]).
 * @property totalNegotiatedDiscount soma de [Quote.negotiatedDiscount]: desconto líquido
 *   concedido na negociação (preços fechados acima da tabela abatem desta soma).
 * @property openQuoteCount orçamentos enviados que o cliente ainda não fechou.
 * @property openQuoteTotal quanto somam esses orçamentos em aberto (peça + serviços, sem frete).
 * @property conversionRate dos pedidos criados no período, quantos o cliente fechou (cancelados contam
 *   como não fechados), ou `null` sem nenhum.
 * @property printHours horas de máquina dos pedidos vendidos (tempo de uma peça × quantidade).
 * @property profitPerPrintHour [totalProfit] ÷ [printHours]: quanto cada hora de máquina deixou
 *   de lucro. `null` quando nenhum pedido tem tempo de impressão.
 * @property laborHours horas do seu trabalho nos pedidos vendidos que têm tempo informado.
 * @property earningsPerLaborHour (lucro + mão de obra) ÷ [laborHours], só dos pedidos com tempo de
 *   trabalho: o que você de fato levou por hora trabalhada, pra comparar com a hora configurada.
 *   A mão de obra já está dentro do custo; sem somá-la de volta, o número seria só o lucro acima
 *   do que você se pagou. `null` quando nenhum pedido tem tempo de trabalho.
 * @property topProducts peças que mais deram lucro, até [QuoteSummary.RANKING_SIZE].
 * @property topDiscountClients clientes que mais receberam desconto na negociação, até
 *   [QuoteSummary.RANKING_SIZE]. Uso interno, nunca exportado.
 */
data class QuoteSummary(
    val quoteCount: Int,
    val totalSalePrice: Double,
    val totalProfit: Double,
    val mostUsedFilamentName: String?,
    val mostUsedFilamentCount: Int,
    val negotiatedCount: Int = 0,
    val totalNegotiatedDiscount: Double = 0.0,
    val openQuoteCount: Int = 0,
    val openQuoteTotal: Double = 0.0,
    val conversionRate: Double? = null,
    val printHours: Double = 0.0,
    val profitPerPrintHour: Double? = null,
    val laborHours: Double = 0.0,
    val earningsPerLaborHour: Double? = null,
    val topProducts: List<ProductRanking> = emptyList(),
    val topDiscountClients: List<ClientDiscountRanking> = emptyList(),
) {
    companion object {
        /** Quantas linhas cada ranking do Dashboard mostra. */
        const val RANKING_SIZE = 5

        val EMPTY = QuoteSummary(
            quoteCount = 0,
            totalSalePrice = 0.0,
            totalProfit = 0.0,
            mostUsedFilamentName = null,
            mostUsedFilamentCount = 0,
        )
    }
}

/**
 * Uma peça no ranking de lucro. Pedidos com o mesmo nome (ignorando maiúsculas e espaços nas
 * pontas) são a mesma peça: é assim que uma reimpressão duplicada do Histórico cai junto.
 *
 * @property name nome como aparece no pedido mais recente.
 * @property profitPerPrintHour lucro ÷ horas de máquina desta peça, `null` sem tempo de impressão.
 *   É o que responde "qual peça vale a pena repetir".
 */
data class ProductRanking(
    val name: String,
    val orderCount: Int,
    val totalProfit: Double,
    val profitPerPrintHour: Double?,
)

/**
 * Um cliente no ranking de desconto: soma de [Quote.negotiatedDiscount] dos pedidos dele.
 *
 * @property negotiatedCount quantos pedidos dele tiveram o preço negociado.
 */
data class ClientDiscountRanking(
    val clientName: String,
    val negotiatedCount: Int,
    val totalDiscount: Double,
)
