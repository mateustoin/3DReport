package com.threedreport.core.report

import com.threedreport.core.model.ClientDiscountRanking
import com.threedreport.core.model.ProductRanking
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote

/**
 * Agregação de orçamentos salvos, pra a tela de Dashboard.
 *
 * Função pura e sem estado, igual a [com.threedreport.core.pricing.PricingCalculator]:
 * mesma entrada, mesma saída. Quem separa venda de orçamento em aberto é esta função
 * ([com.threedreport.core.model.OrderStatus.isSold]), e também quem deixa os produtos do catálogo de
 * fora ([SavedQuote.isOrder], decisão 101): produto não é venda nem orçamento em aberto.
 *
 * O período (decisão 106) conta cada coisa pela data que importa pra ela: venda pela data em que o
 * cliente fechou ([SavedQuote.soldAtEpochMillis]), orçamento em aberto e conversão pela data em que
 * o orçamento foi criado. Um orçamento de agosto aprovado em setembro é venda de setembro.
 */
object QuoteReport {

    /**
     * @param periodStartEpochMillis começo do período, ou `null` pra tudo.
     * @param periodEndEpochMillis fim do período (exclusivo), ou `null` pra até agora.
     */
    fun summarize(
        savedQuotes: List<SavedQuote>,
        periodStartEpochMillis: Long? = null,
        periodEndEpochMillis: Long? = null,
    ): QuoteSummary {
        fun inPeriod(epochMillis: Long) =
            (periodStartEpochMillis == null || epochMillis >= periodStartEpochMillis) &&
                (periodEndEpochMillis == null || epochMillis < periodEndEpochMillis)

        val orders = savedQuotes.filter { it.isOrder }
        val sold = orders.filter { order -> order.soldAtEpochMillis?.let(::inPeriod) == true }
        val created = orders.filter { inPeriod(it.savedAtEpochMillis) }
        val open = created.filter { it.status.isOpen }
        if (sold.isEmpty() && created.isEmpty()) return QuoteSummary.EMPTY

        // Conta pedidos que usaram cada filamento: um pedido multicolor ou com várias impressões conta
        // uma vez pra cada filamento diferente, e não uma vez por impressão.
        val mostUsedFilament = sold
            .flatMap { order -> order.quote.prints.flatMap { print -> print.job.filaments.map { it.filament.name } }.distinct() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }

        val totalProfit = sold.sumOf { it.quote.profit }
        val printHours = sold.sumOf { it.totalPrintTimeMinutes } / 60.0

        val withLabor = sold.filter { it.quote.laborMinutes > 0 }
        val laborHours = withLabor.sumOf { it.quote.laborMinutes } / 60.0
        val laborEarnings = withLabor.sumOf { it.quote.profit + it.quote.costs.labor }

        return QuoteSummary(
            quoteCount = sold.size,
            totalSalePrice = sold.sumOf { it.totalWithServices - it.shippingCost },
            totalProfit = totalProfit,
            mostUsedFilamentName = mostUsedFilament?.key,
            mostUsedFilamentCount = mostUsedFilament?.value ?: 0,
            negotiatedCount = sold.count { it.isNegotiatedWithClient },
            totalNegotiatedDiscount = sold.sumOf { it.clientDiscount },
            openQuoteCount = open.size,
            openQuoteTotal = open.sumOf { it.totalWithServices - it.shippingCost },
            conversionRate = if (created.isEmpty()) null else created.count { it.status.isSold }.toDouble() / created.size,
            printHours = printHours,
            profitPerPrintHour = perHour(totalProfit, printHours),
            laborHours = laborHours,
            earningsPerLaborHour = perHour(laborEarnings, laborHours),
            topProducts = topProducts(sold),
            topDiscountClients = topDiscountClients(sold),
        )
    }

    /**
     * Nome automático ("Orçamento - 24/09/2026 14:30") fica fora: cada um seria uma "peça"
     * diferente e o ranking viraria uma lista de datas.
     *
     * Vendas do mesmo produto do catálogo juntam pelo produto de origem
     * ([SavedQuote.sourceProductId], decisão 103), mesmo que o nome tenha mudado. Pedido sem origem
     * com o nome de um produto vendido entra junto dele (uma reimpressão duplicada antes de a origem
     * existir); o resto continua juntando pelo nome.
     */
    private fun topProducts(sold: List<SavedQuote>): List<ProductRanking> {
        val named = sold.filterNot { it.hasAutoName }
        val sourceByName = named
            .filter { it.sourceProductId != null }
            .sortedBy { it.savedAtEpochMillis }
            .associate { it.name.trim().lowercase() to it.sourceProductId!! }
        return named
            .groupBy { order ->
                val source = order.sourceProductId ?: sourceByName[order.name.trim().lowercase()]
                source?.let { "produto:$it" } ?: "nome:${order.name.trim().lowercase()}"
            }
            .values
            .map { orders ->
                val profit = orders.sumOf { it.quote.profit }
                ProductRanking(
                    name = orders.maxBy { it.savedAtEpochMillis }.name.trim(),
                    orderCount = orders.size,
                    totalProfit = profit,
                    profitPerPrintHour = perHour(profit, orders.sumOf { it.totalPrintTimeMinutes } / 60.0),
                )
            }
            .sortedByDescending { it.totalProfit }
            .take(QuoteSummary.RANKING_SIZE)
    }

    private fun topDiscountClients(sold: List<SavedQuote>): List<ClientDiscountRanking> =
        sold.filter { it.client != null && it.isNegotiatedWithClient }
            .groupBy { it.client!!.name.trim().lowercase() }
            .values
            .map { orders ->
                ClientDiscountRanking(
                    clientName = orders.maxBy { it.savedAtEpochMillis }.client!!.name.trim(),
                    negotiatedCount = orders.size,
                    totalDiscount = orders.sumOf { it.clientDiscount },
                )
            }
            .filter { it.totalDiscount > 0 }
            .sortedByDescending { it.totalDiscount }
            .take(QuoteSummary.RANKING_SIZE)

    private fun perHour(amount: Double, hours: Double): Double? = if (hours > 0) amount / hours else null
}
