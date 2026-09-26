package com.threedreport.app.ui.history

import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.QuoteKind

/**
 * Filtro aplicado à lista de Pedidos ou do Catálogo: [query] busca por nome do orçamento
 * ou nome do cliente (contains, sem diferenciar maiúsculas/minúsculas),
 * [status] restringe a um andamento específico (`null` = todos), [period] é
 * um atalho de intervalo relativo a hoje. [kind] é fixo em cada tela (decisões 101 e 111):
 * a lista mostra um tipo por vez, e o [status] só vale pra pedidos. [category] só vale pra
 * produtos (decisão 102).
 */
data class HistoryFilter(
    val query: String = "",
    val status: OrderStatus? = null,
    val period: PeriodPreset = PeriodPreset.ALL,
    val kind: QuoteKind = QuoteKind.ORDER,
    val category: CategoryFilter = CategoryFilter.All,
)

/** Recorte da lista de produtos por categoria (decisão 102). */
sealed interface CategoryFilter {
    data object All : CategoryFilter

    /** Produtos que ninguém classificou ainda. */
    data object None : CategoryFilter

    /** Uma categoria, comparada sem diferenciar maiúsculas. */
    data class Named(val name: String) : CategoryFilter
}
