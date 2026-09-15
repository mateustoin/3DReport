package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.pricing.PricingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da tela de Orçamento.
 *
 * Recalcula o [com.threedreport.core.model.Quote] a cada mudança de entrada,
 * usando o filamento/impressora escolhidos e as configurações gerais
 * vigentes nos repositórios compartilhados.
 */
class QuoteViewModel(
    private val filamentRepository: FilamentRepository,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val state = MutableStateFlow(
        QuoteUiState(
            filaments = filamentRepository.filaments.value,
            selectedFilament = filamentRepository.filaments.value.firstOrNull(),
            printers = printerRepository.printers.value,
            selectedPrinter = printerRepository.printers.value.firstOrNull(),
        )
    )
    val uiState: StateFlow<QuoteUiState> = state.asStateFlow()

    fun selectFilament(filament: Filament) {
        state.value = state.value.copy(selectedFilament = filament)
        recalculate()
    }

    fun selectPrinter(printer: PrinterProfile) {
        state.value = state.value.copy(selectedPrinter = printer)
        recalculate()
    }

    fun setLengthMeters(text: String) {
        state.value = state.value.copy(lengthMetersText = text)
        recalculate()
    }

    fun setPrintTimeMinutes(text: String) {
        state.value = state.value.copy(printTimeMinutesText = text)
        recalculate()
    }

    private fun recalculate() {
        val current = state.value
        val filament = current.selectedFilament
        val printer = current.selectedPrinter
        val length = parseDecimal(current.lengthMetersText)
        val time = parseDecimal(current.printTimeMinutesText)

        if (filament == null || printer == null || length == null || time == null) {
            state.value = current.copy(quote = null, errorMessage = null)
            return
        }

        val quote = runCatching {
            PricingCalculator.calculate(
                job = PrintJob(
                    filament = filament,
                    filamentLengthMeters = length,
                    printTimeMinutes = time,
                ),
                printer = printer,
                settings = settingsRepository.settings.value,
            )
        }

        state.value = current.copy(
            quote = quote.getOrNull(),
            errorMessage = quote.exceptionOrNull()?.message,
        )
    }
}
