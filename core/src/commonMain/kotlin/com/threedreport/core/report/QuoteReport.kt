package com.threedreport.core.report

import com.threedreport.core.model.ClientDiscountRanking
import com.threedreport.core.model.ProductRanking
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote

/**
 * Agregação de orçamentos salvos, pra a tela de Dashboard.
 *
 * Função pura e sem estado, igual a [com.threedreport.core.pricing.PricingCalculator]:
 * mesma entrada, mesma saída. Quem filtra por período (ex.: só os últimos 30
 * dias) é quem chama, passando a lista já filtrada. Quem separa venda de orçamento em aberto é
 * esta função ([com.threedreport.core.model.OrderStatus.isSold]), e também quem deixa os produtos do catálogo de fora
 * ([SavedQuote.isOrder], decisão 101): produto não é venda nem orçamento em aberto.
 */
object QuoteReport {

    fun summarize(savedQuotes: List<SavedQuote>): QuoteSummary {
        val quotes = savedQuotes.filter { it.isOrder }
        if (quotes.isEmpty()) return QuoteSummary.EMPTY

        val (sold, open) = quotes.partition { it.status.isSold }

        val mostUsedFilament = sold
            .groupingBy { it.quote.job.filament.name }
            .eachCount()
            .maxByOrNull { it.value }

        val totalProfit = sold.sumOf { it.quote.profit }
        val printHours = sold.sumOf { it.totalPrintTimeMinutes } / 60.0

        val withLabor = sold.filter { it.quote.totalLaborMinutes > 0 }
        val laborHours = withLabor.sumOf { it.quote.totalLaborMinutes } / 60.0
        val laborEarnings = withLabor.sumOf { it.quote.profit + it.quote.costs.labor }

        return QuoteSummary(
            quoteCount = sold.size,
            totalSalePrice = sold.sumOf { it.totalWithServices },
            totalProfit = totalProfit,
            mostUsedFilamentName = mostUsedFilament?.key,
            mostUsedFilamentCount = mostUsedFilament?.value ?: 0,
            negotiatedCount = sold.count { it.quote.isNegotiated },
            totalNegotiatedDiscount = sold.sumOf { it.quote.negotiatedDiscount },
            openQuoteCount = open.size,
            openQuoteTotal = open.sumOf { it.totalWithServices },
            conversionRate = sold.size.toDouble() / quotes.size,
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
        sold.filter { it.client != null && it.quote.isNegotiated }
            .groupBy { it.client!!.name.trim().lowercase() }
            .values
            .map { orders ->
                ClientDiscountRanking(
                    clientName = orders.maxBy { it.savedAtEpochMillis }.client!!.name.trim(),
                    negotiatedCount = orders.size,
                    totalDiscount = orders.sumOf { it.quote.negotiatedDiscount },
                )
            }
            .filter { it.totalDiscount > 0 }
            .sortedByDescending { it.totalDiscount }
            .take(QuoteSummary.RANKING_SIZE)

    private fun perHour(amount: Double, hours: Double): Double? = if (hours > 0) amount / hours else null
}
