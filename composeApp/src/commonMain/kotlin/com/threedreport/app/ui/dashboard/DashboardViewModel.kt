package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.periodEndEpochMillis
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.core.report.CatalogReport
import com.threedreport.core.report.QuoteReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel do Dashboard: vendas, lucro por hora e rankings, recortados por período, num [uiState] só. Nenhuma conta
 * é feita aqui: tudo vem de [QuoteReport] e [CatalogReport]. As configurações entram só pra comparar o que o seu
 * trabalho rendeu com a hora que você configurou. Os repositórios são acompanhados dentro de [scope].
 */
class DashboardViewModel(
    private val repository: QuoteHistoryRepository,
    private val settingsRepository: SettingsRepository,
    scope: CoroutineScope,
) {
    private var period = PeriodPreset.ALL
    private val state = MutableStateFlow(build())
    val uiState: StateFlow<DashboardUiState> = state.asStateFlow()

    init {
        combine(repository.savedQuotes, settingsRepository.settings) { _, _ -> }
            .onEach { state.value = build() }
            .launchIn(scope)
    }

    fun setPeriod(preset: PeriodPreset) {
        period = preset
        state.value = build()
    }

    /**
     * Venda conta pela data em que foi fechada, e não pela data do orçamento (decisão 106): um orçamento de março
     * aprovado em abril é venda de abril. O catálogo (decisão 103) não tem recorte de período: é o que se oferece hoje.
     */
    private fun build(): DashboardUiState {
        val savedQuotes = repository.savedQuotes.value
        val summary = QuoteReport.summarize(savedQuotes, periodStartEpochMillis(period), periodEndEpochMillis(period))
        val mode = when {
            summary.quoteCount > 0 -> DashboardMode.SALES
            summary.openQuoteCount > 0 -> DashboardMode.NO_SALES
            else -> DashboardMode.NOTHING_SAVED
        }
        val catalog = if (mode == DashboardMode.SALES) null else CatalogReport.summarize(savedQuotes).takeIf { it.productCount > 0 }
        return DashboardUiState(
            period = period,
            mode = mode,
            summary = summary,
            catalog = catalog,
            laborRatePerHour = settingsRepository.settings.value.laborRatePerHour,
        )
    }
}
