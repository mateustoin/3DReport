package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.periodEndEpochMillis
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.report.CatalogReport
import com.threedreport.core.report.QuoteReport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel do Dashboard: vendas, lucro por hora e rankings, recortados por período, num [uiState] só. Nenhuma conta
 * é feita aqui: tudo vem de [QuoteReport] e [CatalogReport]. As configurações entram só pra comparar o que o seu
 * trabalho rendeu com a hora que você configurou.
 *
 * O resumo só é recalculado enquanto há quem observe o [uiState] (a tela aberta) e fora da thread da tela, em [io]. A
 * tela fechada não paga a conta a cada pedido salvo; ao voltar, o que mudou nesse meio tempo é recalculado uma vez.
 */
class DashboardViewModel(
    private val repository: QuoteHistoryRepository,
    private val settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val period = MutableStateFlow(PeriodPreset.ALL)

    /** O que o último resumo foi calculado a partir de. Serve pra não recalcular quando nada mudou (a criação, a volta à tela). */
    private class Inputs(val quotes: List<SavedQuote>, val laborRatePerHour: Double, val period: PeriodPreset) {
        fun sameAs(other: Inputs) = quotes === other.quotes && laborRatePerHour == other.laborRatePerHour && period == other.period
    }

    @Volatile
    private var last: Pair<Inputs, DashboardUiState>

    /** Quantas vezes o resumo foi calculado. Só pros testes conferirem que a tela fechada não gasta conta. */
    internal var buildCount = 0
        private set

    init {
        last = currentInputs().let { it to build(it) }
    }

    val uiState: StateFlow<DashboardUiState> = combine(repository.savedQuotes, settingsRepository.settings, period) { quotes, settings, preset ->
        Inputs(quotes, settings.laborRatePerHour, preset)
    }
        .map { inputs -> last.takeIf { it.first.sameAs(inputs) }?.second ?: build(inputs).also { last = inputs to it } }
        .flowOn(io)
        .stateIn(scope, SharingStarted.WhileSubscribed(), last.second)

    fun setPeriod(preset: PeriodPreset) {
        period.value = preset
    }

    private fun currentInputs() = Inputs(repository.savedQuotes.value, settingsRepository.settings.value.laborRatePerHour, period.value)

    /**
     * Venda conta pela data em que foi fechada, e não pela data do orçamento (decisão 106): um orçamento de março
     * aprovado em abril é venda de abril. O catálogo (decisão 103) não tem recorte de período: é o que se oferece hoje.
     */
    private fun build(inputs: Inputs): DashboardUiState {
        buildCount++
        val savedQuotes = inputs.quotes
        val preset = inputs.period
        val summary = QuoteReport.summarize(savedQuotes, periodStartEpochMillis(preset), periodEndEpochMillis(preset))
        val mode = when {
            summary.quoteCount > 0 -> DashboardMode.SALES
            summary.openQuoteCount > 0 -> DashboardMode.NO_SALES
            else -> DashboardMode.NOTHING_SAVED
        }
        val catalog = if (mode == DashboardMode.SALES) null else CatalogReport.summarize(savedQuotes).takeIf { it.productCount > 0 }
        return DashboardUiState(
            period = preset,
            mode = mode,
            summary = summary,
            catalog = catalog,
            laborRatePerHour = inputs.laborRatePerHour,
            autoNamedProducts = if (catalog == null) emptyMap() else savedQuotes.filter { it.autoNamed && !it.isOrder }.associate { it.name.trim() to it.savedAtEpochMillis },
        )
    }
}
