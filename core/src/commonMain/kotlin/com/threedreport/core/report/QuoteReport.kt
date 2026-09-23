package com.threedreport.core.report

import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote

/**
 * Agregação de orçamentos salvos, pra a tela de Dashboard.
 *
 * Função pura e sem estado, igual a [com.threedreport.core.pricing.PricingCalculator]:
 * mesma entrada, mesma saída. Quem filtra por período (ex.: só os últimos 30
 * dias) é quem chama, passando a lista já filtrada.
 */
object QuoteReport {

    fun summarize(quotes: List<SavedQuote>): QuoteSummary {
        if (quotes.isEmpty()) return QuoteSummary.EMPTY

        val mostUsedFilament = quotes
            .groupingBy { it.quote.job.filament.name }
            .eachCount()
            .maxByOrNull { it.value }

        return QuoteSummary(
            quoteCount = quotes.size,
            totalSalePrice = quotes.sumOf { it.totalWithServices },
            totalProfit = quotes.sumOf { it.quote.profit },
            mostUsedFilamentName = mostUsedFilament?.key,
            mostUsedFilamentCount = mostUsedFilament?.value ?: 0,
            negotiatedCount = quotes.count { it.quote.isNegotiated },
            totalNegotiatedDiscount = quotes.sumOf { it.quote.negotiatedDiscount },
        )
    }
}
