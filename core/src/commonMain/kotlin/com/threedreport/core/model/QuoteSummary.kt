package com.threedreport.core.model

/**
 * Agregação de uma lista de [SavedQuote] (ver [com.threedreport.core.report.QuoteReport]),
 * usada no Dashboard.
 *
 * @property quoteCount quantos orçamentos entraram na soma.
 * @property totalSalePrice soma de [SavedQuote.totalWithServices] (venda + serviços).
 * @property totalProfit soma de [Quote.profit].
 * @property mostUsedFilamentName nome do filamento mais usado no período, ou
 *   `null` se não houver nenhum orçamento.
 * @property mostUsedFilamentCount quantos orçamentos usaram [mostUsedFilamentName].
 */
data class QuoteSummary(
    val quoteCount: Int,
    val totalSalePrice: Double,
    val totalProfit: Double,
    val mostUsedFilamentName: String?,
    val mostUsedFilamentCount: Int,
) {
    companion object {
        val EMPTY = QuoteSummary(
            quoteCount = 0,
            totalSalePrice = 0.0,
            totalProfit = 0.0,
            mostUsedFilamentName = null,
            mostUsedFilamentCount = 0,
        )
    }
}
