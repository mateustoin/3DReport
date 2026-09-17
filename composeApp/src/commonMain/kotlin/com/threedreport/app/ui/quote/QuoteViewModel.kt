package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.pickImageFile
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel da tela de Orçamento.
 *
 * [filaments], [printers], [services] e [settings] são repassados diretamente
 * dos repositórios compartilhados (sem cópia), então refletem na hora
 * qualquer cadastro/edição feito nas outras telas. [calculate] é uma função
 * pura, chamada pela tela a cada recomposição com os valores atuais desses
 * fluxos.
 */
class QuoteViewModel(
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
    serviceRepository: ServiceRepository,
    private val historyRepository: QuoteHistoryRepository,
) {
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings
    val services: StateFlow<List<Service>> = serviceRepository.services

    private val inputState = MutableStateFlow(QuoteInputState())
    val input: StateFlow<QuoteInputState> = inputState.asStateFlow()

    private val saveFormState = MutableStateFlow(SaveQuoteFormState())
    val saveForm: StateFlow<SaveQuoteFormState> = saveFormState.asStateFlow()

    fun selectFilament(id: String) = inputState.update { it.copy(filamentId = id, filamentColorId = null) }
    fun selectFilamentColor(id: String) = inputState.update { it.copy(filamentColorId = id) }
    fun selectPrinter(id: String) = inputState.update { it.copy(printerId = id) }
    fun setLengthMeters(text: String) = inputState.update { it.copy(lengthMetersText = text) }
    fun setPrintTimeMinutes(text: String) = inputState.update { it.copy(printTimeMinutesText = text) }

    fun toggleService(id: String) = inputState.update {
        it.copy(selectedServiceIds = if (id in it.selectedServiceIds) it.selectedServiceIds - id else it.selectedServiceIds + id)
    }

    fun setAppliesMarketplaceFee(applies: Boolean) = inputState.update { it.copy(appliesMarketplaceFee = applies) }

    fun setSaveName(text: String) = saveFormState.update { it.copy(name = text, savedConfirmation = false) }
    fun setSourceLink(text: String) = saveFormState.update { it.copy(sourceLink = text, savedConfirmation = false) }
    fun setClientName(text: String) = saveFormState.update { it.copy(clientName = text, savedConfirmation = false) }
    fun setClientContact(text: String) = saveFormState.update { it.copy(clientContact = text, savedConfirmation = false) }
    fun clearPhoto() = saveFormState.update { it.copy(photo = null, savedConfirmation = false) }

    fun pickPhoto() {
        val picked = pickImageFile() ?: return
        saveFormState.update { it.copy(photo = picked, savedConfirmation = false) }
    }

    fun saveQuote(quote: Quote, services: List<Service>) {
        val form = saveFormState.value
        val client = form.clientName.trim().ifEmpty { null }?.let { name ->
            Client(name = name, contact = form.clientContact.trim().ifEmpty { null })
        }
        historyRepository.save(form.name, quote, services, form.photo, form.sourceLink, client)
        saveFormState.value = SaveQuoteFormState(savedConfirmation = true)
    }

    fun calculate(
        filaments: List<Filament>,
        printers: List<PrinterProfile>,
        settings: PricingSettings,
        services: List<Service>,
        input: QuoteInputState,
    ): QuoteResult {
        val filament = filaments.find { it.id == input.filamentId } ?: filaments.firstOrNull()
        val printer = printers.find { it.id == input.printerId } ?: printers.firstOrNull()
        val selectedServices = services.filter { it.id in input.selectedServiceIds }
        val availableColors = filament?.colors?.filter { it.inStock }.orEmpty()
        val filamentColor = availableColors.find { it.id == input.filamentColorId } ?: availableColors.firstOrNull()
        val length = parseDecimal(input.lengthMetersText)
        val time = parseDecimal(input.printTimeMinutesText)

        if (filament == null || printer == null || length == null || time == null) {
            return QuoteResult(filament = filament, filamentColor = filamentColor, printer = printer, selectedServices = selectedServices)
        }

        val job = PrintJob(filament = filament, filamentLengthMeters = length, printTimeMinutes = time, filamentColor = filamentColor)
        return runCatching {
            PricingCalculator.calculate(job, printer, settings, input.appliesMarketplaceFee)
        }.fold(
            onSuccess = { QuoteResult(filament, filamentColor, printer, quote = it, selectedServices = selectedServices) },
            onFailure = { QuoteResult(filament, filamentColor, printer, errorMessage = it.message, selectedServices = selectedServices) },
        )
    }
}
