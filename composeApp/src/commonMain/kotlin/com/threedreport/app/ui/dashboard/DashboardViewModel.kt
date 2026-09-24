package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.PricingSettings
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote
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

    /** Função pura: agrega [savedQuotes] recortados por [period]. Chamada pela tela com os valores já coletados. */
    fun summarize(savedQuotes: List<SavedQuote>, period: PeriodPreset): QuoteSummary {
        val startEpochMillis = periodStartEpochMillis(period)
        val filtered = if (startEpochMillis == null) {
            savedQuotes
        } else {
            savedQuotes.filter { it.savedAtEpochMillis >= startEpochMillis }
        }
        return QuoteReport.summarize(filtered)
    }
}
