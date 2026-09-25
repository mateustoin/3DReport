package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.PricingSettings
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.periodEndEpochMillis
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.report.CatalogReport
import com.threedreport.core.report.CatalogSummary
import com.threedreport.core.report.QuoteReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da tela de Dashboard: vendas, lucro por hora e rankings, recortados por período. Lê as
 * configurações só pra comparar o que o seu trabalho rendeu com a hora que você configurou.
 */
class DashboardViewModel(repository: QuoteHistoryRepository, settingsRepository: SettingsRepository) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes
    val settings: StateFlow<PricingSettings> = settingsRepository.settings

    private val periodState = MutableStateFlow(PeriodPreset.ALL)
    val period: StateFlow<PeriodPreset> = periodState.asStateFlow()

    fun setPeriod(preset: PeriodPreset) {
        periodState.value = preset
    }

    /**
     * Função pura: agrega [savedQuotes] no [period]. Venda conta pela data em que foi fechada, e não pela
     * data do orçamento (decisão 106): um orçamento de março aprovado em abril é venda de abril.
     */
    fun summarize(savedQuotes: List<SavedQuote>, period: PeriodPreset): QuoteSummary =
        QuoteReport.summarize(savedQuotes, periodStartEpochMillis(period), periodEndEpochMillis(period))

    /**
     * Resumo do catálogo (decisão 103), mostrado quando não há vendas no período. Sem recorte de
     * período: o catálogo é o que se oferece hoje.
     */
    fun catalog(savedQuotes: List<SavedQuote>): CatalogSummary = CatalogReport.summarize(savedQuotes)
}
