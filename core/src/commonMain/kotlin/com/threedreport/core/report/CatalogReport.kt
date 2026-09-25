package com.threedreport.core.report

import com.threedreport.core.model.SavedQuote

/**
 * Um produto no resumo do catálogo.
 *
 * @property unitPrice preço de uma unidade, o que vai pro catálogo (o anunciado, quando houver).
 * @property profit lucro de uma unidade nesse preço.
 * @property profitPerPrintHour lucro por hora de máquina, `null` sem tempo de impressão. É o que
 *   responde a pergunta de quem está começando: qual peça vale a pena oferecer primeiro.
 */
data class CatalogProductStat(
    val name: String,
    val unitPrice: Double,
    val profit: Double,
    val profitPerPrintHour: Double?,
)

/**
 * @property products do melhor lucro por hora de máquina pro pior; sem tempo de impressão, no fim.
 */
data class CatalogSummary(
    val productCount: Int,
    val minUnitPrice: Double?,
    val maxUnitPrice: Double?,
    val products: List<CatalogProductStat>,
)

/**
 * Resumo do catálogo pro Dashboard de quem ainda não tem vendas (decisão 103). Função pura, no
 * estilo de [QuoteReport]. Sem recorte de período: catálogo é o que se oferece hoje, não histórico.
 * Pedidos ficam de fora ([SavedQuote.isOrder]).
 */
object CatalogReport {

    fun summarize(savedQuotes: List<SavedQuote>): CatalogSummary {
        val products = savedQuotes.filterNot { it.isOrder }
        val stats = products.map { product ->
            val quote = product.quote
            val hours = product.totalPrintTimeMinutes / 60.0
            CatalogProductStat(
                name = product.name.trim(),
                unitPrice = quote.unitSalePrice,
                profit = quote.profit / quote.quantity,
                profitPerPrintHour = if (hours > 0) quote.profit / hours else null,
            )
        }
        return CatalogSummary(
            productCount = stats.size,
            minUnitPrice = stats.minOfOrNull { it.unitPrice },
            maxUnitPrice = stats.maxOfOrNull { it.unitPrice },
            products = stats.sortedByDescending { it.profitPerPrintHour ?: Double.NEGATIVE_INFINITY },
        )
    }
}
