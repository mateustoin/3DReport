package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.report.QuoteReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel da tela de Dashboard: total vendido, lucro e filamento mais usado, recortado por período. */
class DashboardViewModel(repository: QuoteHistoryRepository) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

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
