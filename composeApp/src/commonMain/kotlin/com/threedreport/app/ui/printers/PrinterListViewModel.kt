package com.threedreport.app.ui.printers

import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.app.ui.format.toRequiredInt
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PrinterMaintenance
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.report.ComponentStatus
import com.threedreport.core.report.MaintenanceReport
import com.threedreport.core.report.PrintQueueReport
import com.threedreport.core.report.PrinterQueueEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela de Impressoras: lista os perfis salvos e edita um por vez
 * em [form] (nulo quando nenhum formulário está aberto). Cuida também da manutenção de cada
 * impressora (decisão 96), que fica nesta aba em vez de numa aba própria.
 */
class PrinterListViewModel(
    private val repository: PrinterRepository,
    historyRepository: QuoteHistoryRepository,
    private val maintenanceRepository: MaintenanceRepository,
) {

    val printers: StateFlow<List<PrinterProfile>> = repository.printers
    val savedQuotes: StateFlow<List<SavedQuote>> = historyRepository.savedQuotes
    val maintenance: StateFlow<PrinterMaintenance> = maintenanceRepository.maintenance

    /** Função pura: quanto cada impressora está ocupada agora (ver [PrintQueueReport]). */
    fun printQueue(printers: List<PrinterProfile>, savedQuotes: List<SavedQuote>): List<PrinterQueueEntry> =
        PrintQueueReport.summarize(printers, savedQuotes)

    private val formState = MutableStateFlow<PrinterFormState?>(null)
    val form: StateFlow<PrinterFormState?> = formState.asStateFlow()

    fun startAdd() {
        formState.value = PrinterFormState()
    }

    /** Abre o formulário de nova impressora já preenchido com um preset — ver [PrinterPreset]. */
    fun startAddFromPreset(preset: PrinterPreset) {
        formState.value = PrinterFormState(
            name = "${preset.brand} ${preset.model}",
            printerPowerWattsText = preset.ratedPowerWatts.toString(),
        )
    }

    fun startEdit(printer: PrinterProfile) {
        formState.value = printer.toFormState()
    }

    fun cancelEdit() {
        formState.value = null
    }

    fun updateForm(transform: (PrinterFormState) -> PrinterFormState) {
        formState.value = formState.value?.let { transform(it).copy(errorMessage = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value ?: return
        val result = runCatching {
            PrinterProfile(
                id = current.id ?: Uuid.random().toString(),
                name = current.name.trim().ifEmpty { error("Nome não pode ser vazio") },
                printerPowerWatts = current.printerPowerWattsText.toRequiredDouble("Consumo"),
                maintenanceCostPerHour = current.maintenanceCostPerHourText.toRequiredDouble("Manutenção por hora"),
                machineInvestment = MachineInvestment(
                    machinePrice = current.machinePriceText.toRequiredDouble("Valor da máquina"),
                    paybackMonths = current.paybackMonthsText.toRequiredInt("Prazo de retorno"),
                    printingDaysPerMonth = current.printingDaysPerMonthText.toRequiredInt("Dias de uso por mês"),
                    printingHoursPerDay = current.printingHoursPerDayText.toRequiredDouble("Horas de uso por dia"),
                ),
            )
        }

        result.fold(
            onSuccess = { printer ->
                if (current.id == null) repository.add(printer) else repository.update(printer)
                formState.value = null
            },
            onFailure = { formState.value = current.copy(errorMessage = it.message) },
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        maintenanceRepository.deleteAllFor(id)
        if (formState.value?.id == id) formState.value = null
    }

    // --- Manutenção (decisão 96) ---

    /** Função pura: horas de uso de [printerId] (ver [MaintenanceReport.printerHours]). */
    fun printerHours(printerId: String, savedQuotes: List<SavedQuote>, maintenance: PrinterMaintenance): Double =
        MaintenanceReport.printerHours(printerId, savedQuotes, maintenance.manualUsage)

    /** Função pura: situação de cada componente de [printerId], o mais urgente primeiro. */
    fun componentStatuses(printerId: String, savedQuotes: List<SavedQuote>, maintenance: PrinterMaintenance): List<ComponentStatus> {
        val hours = printerHours(printerId, savedQuotes, maintenance)
        return maintenance.components
            .filter { it.printerId == printerId }
            .map { MaintenanceReport.componentStatus(it, hours) }
            .sortedBy { it.remainingHours }
    }

    private fun currentHours(printerId: String) = printerHours(printerId, savedQuotes.value, maintenance.value)

    /**
     * Cadastra um componente. [hoursSinceText] vazio conta como zero (peça nova ou trocada agora).
     * Devolve a mensagem de erro, ou `null` quando deu certo.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun addComponent(printerId: String, name: String, intervalText: String, hoursSinceText: String): String? = runCatching {
        val hoursSince = if (hoursSinceText.isBlank()) 0.0 else hoursSinceText.toRequiredDouble("Horas desde a última vez")
        require(hoursSince >= 0) { "Horas desde a última vez não pode ser negativo" }
        maintenanceRepository.addComponent(
            MaintenanceComponent(
                id = Uuid.random().toString(),
                printerId = printerId,
                name = name.trim().ifEmpty { error("Informe o nome do componente") },
                intervalHours = parseInterval(intervalText),
                hoursAtLastService = MaintenanceReport.hoursAtLastServiceFor(currentHours(printerId), hoursSince),
            ),
        )
    }.exceptionOrNull()?.message

    /** Muda nome e intervalo, sem mexer no contador. Devolve a mensagem de erro, ou `null`. */
    fun updateComponent(component: MaintenanceComponent, name: String, intervalText: String): String? = runCatching {
        maintenanceRepository.updateComponent(
            component.copy(
                name = name.trim().ifEmpty { error("Informe o nome do componente") },
                intervalHours = parseInterval(intervalText),
            ),
        )
    }.exceptionOrNull()?.message

    fun deleteComponent(id: String) = maintenanceRepository.deleteComponent(id)

    /** "Feito hoje": registra no diário com o nome do componente e recomeça o contador dele. */
    fun markServiced(component: MaintenanceComponent) {
        logService(component.printerId, todayEpochDay(), component.name, component.id)
    }

    /**
     * Registra uma manutenção no diário. Com [componentId], o contador daquele componente recomeça
     * agora, mesmo que a data informada seja antiga: o app não sabe quantas horas a máquina rodou
     * desde então. Devolve a mensagem de erro, ou `null`.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun logService(printerId: String, dateEpochDay: Long, description: String, componentId: String?): String? = runCatching {
        maintenanceRepository.logService(
            MaintenanceLogEntry(
                id = Uuid.random().toString(),
                printerId = printerId,
                dateEpochDay = dateEpochDay,
                description = description.trim().ifEmpty { error("Descreva o que foi feito") },
                componentId = componentId,
            ),
            printerHoursNow = currentHours(printerId),
        )
    }.exceptionOrNull()?.message

    fun deleteLogEntry(id: String) = maintenanceRepository.deleteLogEntry(id)

    /** Lança horas de uso fora de orçamento, com a data de hoje. Devolve a mensagem de erro, ou `null`. */
    @OptIn(ExperimentalUuidApi::class)
    fun addManualUsage(printerId: String, hoursText: String, reason: String): String? = runCatching {
        val hours = hoursText.toRequiredDouble("Horas")
        require(hours > 0) { "Horas deve ser maior que zero" }
        maintenanceRepository.addManualUsage(
            ManualUsageEntry(
                id = Uuid.random().toString(),
                printerId = printerId,
                dateEpochDay = todayEpochDay(),
                hours = hours,
                reason = reason.trim().ifEmpty { error("Informe o motivo") },
            ),
        )
    }.exceptionOrNull()?.message

    fun deleteManualUsage(id: String) = maintenanceRepository.deleteManualUsage(id)

    private fun parseInterval(text: String): Double {
        val interval = parseDecimal(text) ?: error("Intervalo inválido")
        require(interval > 0) { "O intervalo deve ser maior que zero" }
        return interval
    }
}
