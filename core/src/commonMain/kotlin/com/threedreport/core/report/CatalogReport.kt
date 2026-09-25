package com.threedreport.core.report

import com.threedreport.core.model.SavedQuote

/**
 * Um produto no resumo do catálogo.
 *
 * @property price o mesmo preço que sai no catálogo em PDF ([SavedQuote.totalWithServices]: o
 *   anunciado, quando houver, com serviços e a quantidade do produto).
 * @property profit lucro do produto nesse preço.
 * @property profitPerPrintHour lucro por hora de máquina, `null` sem tempo de impressão. É o que
 *   responde a pergunta de quem está começando: qual peça vale a pena oferecer primeiro.
 */
data class CatalogProductStat(
    val name: String,
    val price: Double,
    val profit: Double,
    val profitPerPrintHour: Double?,
)

/**
 * @property products do melhor lucro por hora de máquina pro pior; sem tempo de impressão, no fim.
 */
data class CatalogSummary(
    val productCount: Int,
    val minPrice: Double?,
    val maxPrice: Double?,
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
            // Preço como o cliente vê no catálogo em PDF, pra os dois nunca mostrarem números diferentes.
            val hours = product.totalPrintTimeMinutes / 60.0
            CatalogProductStat(
                name = product.name.trim(),
                price = product.totalWithServices,
                profit = quote.profit,
                profitPerPrintHour = if (hours > 0) quote.profit / hours else null,
            )
        }
        return CatalogSummary(
            productCount = stats.size,
            minPrice = stats.minOfOrNull { it.price },
            maxPrice = stats.maxOfOrNull { it.price },
            products = stats.sortedByDescending { it.profitPerPrintHour ?: Double.NEGATIVE_INFINITY },
        )
    }
}
