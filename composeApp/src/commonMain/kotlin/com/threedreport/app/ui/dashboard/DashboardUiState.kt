package com.threedreport.app.ui.dashboard

import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.report.CatalogSummary

/** O que o Dashboard mostra no período escolhido. */
enum class DashboardMode {
    /** Há venda no período: blocos de resumo, ranking de peças e os blocos de pares. */
    SALES,

    /**
     * Há orçamento em aberto, mas nenhuma venda: os blocos de resumo continuam (zerados, pra tela não pular ao trocar
     * de período), com "Nenhuma venda" no lugar do ranking e o que está esperando resposta.
     */
    NO_SALES,

    /** Nada salvo no período (nem venda, nem orçamento em aberto): sem os blocos de resumo. */
    NOTHING_SAVED,
}

/**
 * Estado do Dashboard. Os valores saem do `core` ([QuoteSummary] e [CatalogSummary]) como números; a tela só os
 * formata na moeda do app.
 *
 * @property catalog o catálogo, só quando não há venda no período e há produto salvo (o Dashboard de quem ainda não
 *   vende, decisão 103); `null` nos demais casos.
 * @property laborRatePerHour a hora de trabalho configurada, pra comparar com o que o trabalho rendeu (zero: não configurada).
 */
data class DashboardUiState(
    val period: PeriodPreset = PeriodPreset.ALL,
    val mode: DashboardMode = DashboardMode.NOTHING_SAVED,
    val summary: QuoteSummary = QuoteSummary.EMPTY,
    val catalog: CatalogSummary? = null,
    val laborRatePerHour: Double = 0.0,
) {
    /** Escolheu "Tudo": os textos de vazio não falam em período. */
    val isAllPeriod: Boolean get() = period == PeriodPreset.ALL

    /** Os quatro blocos de resumo aparecem (zerados quando não há venda); só "nada salvo" fica sem eles. */
    val showSummary: Boolean get() = mode != DashboardMode.NOTHING_SAVED

    /** O lucro do período é negativo (prejuízo): a tela o pinta de erro em vez de âmbar. */
    val profitIsNegative: Boolean get() = summary.totalProfit < 0

    /** "Novo orçamento" é a ação principal (âmbar) só na primeira vez: nada salvo e o período "Tudo". */
    val newQuoteIsPrimary: Boolean get() = mode == DashboardMode.NOTHING_SAVED && isAllPeriod
}
