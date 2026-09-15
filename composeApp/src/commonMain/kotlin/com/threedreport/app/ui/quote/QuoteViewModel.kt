package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.pricing.PricingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel da tela de Orçamento.
 *
 * [filaments], [printers] e [settings] são repassados diretamente dos
 * repositórios compartilhados (sem cópia), então refletem na hora qualquer
 * cadastro/edição feito nas outras telas. [calculate] é uma função pura,
 * chamada pela tela a cada recomposição com os valores atuais desses fluxos.
 */
class QuoteViewModel(
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
) {
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings

    private val inputState = MutableStateFlow(QuoteInputState())
    val input: StateFlow<QuoteInputState> = inputState.asStateFlow()

    fun selectFilament(id: String) = inputState.update { it.copy(filamentId = id) }
    fun selectPrinter(id: String) = inputState.update { it.copy(printerId = id) }
    fun setLengthMeters(text: String) = inputState.update { it.copy(lengthMetersText = text) }
    fun setPrintTimeMinutes(text: String) = inputState.update { it.copy(printTimeMinutesText = text) }

    fun calculate(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        input: QuoteInputState,
    ): QuoteResult {
        val filament = filaments.find { it.id == input.filamentId } ?: filaments.firstOrNull()
        val printer = printers.find { it.id == input.printerId } ?: printers.firstOrNull()
        val length = parseDecimal(input.lengthMetersText)
        val time = parseDecimal(input.printTimeMinutesText)

        if (filament == null || printer == null || length == null || time == null) {
            return QuoteResult(filament = filament, printer = printer)
        }

        val job = PrintJob(filament = filament, filamentLengthMeters = length, printTimeMinutes = time)
        return runCatching { PricingCalculator.calculate(job, printer, settings) }.fold(
            onSuccess = { QuoteResult(filament, printer, quote = it) },
            onFailure = { QuoteResult(filament, printer, errorMessage = it.message) },
        )
    }
}
