package com.threedreport.app.ui.dashboard

import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.report.CatalogSummary

/** O que o Dashboard mostra no período escolhido. */
enum class DashboardMode {
    /** Há venda no período: blocos de resumo, ranking de peças e os blocos de pares. */
    SALES,

    /** Há orçamento em aberto, mas nenhuma venda: "Nenhuma venda" e o que está esperando resposta. */
    NO_SALES,

    /** Nada salvo no período (nem venda, nem orçamento em aberto). */
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
)
