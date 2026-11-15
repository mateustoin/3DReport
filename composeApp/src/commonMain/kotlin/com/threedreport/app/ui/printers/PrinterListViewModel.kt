package com.threedreport.app.ui.printers

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.common_error_positive
import _3dreport.composeapp.generated.resources.common_error_required
import _3dreport.composeapp.generated.resources.common_error_whole_number
import _3dreport.composeapp.generated.resources.printers_error_component_name
import _3dreport.composeapp.generated.resources.printers_error_days_max
import _3dreport.composeapp.generated.resources.printers_error_hours_max
import _3dreport.composeapp.generated.resources.printers_error_hours_since
import _3dreport.composeapp.generated.resources.printers_error_interval
import _3dreport.composeapp.generated.resources.printers_error_log_description
import _3dreport.composeapp.generated.resources.printers_error_usage_hours
import _3dreport.composeapp.generated.resources.printers_error_usage_reason
import _3dreport.composeapp.generated.resources.printers_notice_archived
import _3dreport.composeapp.generated.resources.printers_notice_deleted
import _3dreport.composeapp.generated.resources.printers_notice_restored
import _3dreport.composeapp.generated.resources.printers_notice_saved
import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.ui.components.PendingDelete
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.PrinterMaintenance
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.report.ComponentStatus
import com.threedreport.core.report.MaintenanceReport
import com.threedreport.core.report.MaintenanceState
import com.threedreport.core.report.PrintQueueReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** O que a manutenção está pedindo de uma impressora, pelo componente mais urgente. */
data class MaintenanceSummary(
    val componentName: String,
    val state: MaintenanceState,
    val remainingHours: Double,
    /** Quantos outros componentes além deste estão vencidos. */
    val otherOverdue: Int,
)

/** Uma impressora na lista, com o que a linha mostra além do cadastro. */
data class PrinterRowState(
    val printer: PrinterProfile,
    /** Retorno do investimento mais manutenção por hora de impressão, sem a energia (a mesma prévia do formulário). */
    val machineCostPerHour: Double,
    /** Horas de fila de pedidos "Em impressão" nesta impressora, em minutos. */
    val queuedMinutes: Double,
    val queuedQuoteCount: Int,
    /** `null` quando a impressora não tem nenhum componente cadastrado. */
    val maintenance: MaintenanceSummary?,
)

/** O diálogo de manutenção aberto: a impressora, as horas de uso que o app conhece e tudo o que é dela. */
data class MaintenanceDialogState(
    val printer: PrinterProfile,
    val hours: Double,
    /** Situação de cada componente, o mais urgente primeiro. */
    val statuses: List<ComponentStatus>,
    val data: PrinterMaintenance,
)

/**
 * Tudo o que a tela de Impressoras mostra: a lista (ativas e arquivadas), o formulário aberto (nulo quando nenhum), a
 * pergunta de exclusão pendente, o diálogo de manutenção e a escolha da lista pré-cadastrada.
 */
data class PrintersUiState(
    val active: List<PrinterRowState> = emptyList(),
    val archived: List<PrinterRowState> = emptyList(),
    val form: PrinterFormState? = null,
    val pendingDelete: PendingDelete? = null,
    val maintenance: MaintenanceDialogState? = null,
    val presetPickerOpen: Boolean = false,
) {
    /** Nenhuma impressora cadastrada, nem arquivada: a tela mostra o estado vazio com o botão que cria a primeira. */
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

/**
 * ViewModel da tela de Impressoras. Um [uiState] só e os [notices] (avisos de uma frase). Cuida também da manutenção
 * de cada impressora (decisão 96), que fica nesta aba em vez de numa aba própria. A lista, a fila e a manutenção
 * acompanham os repositórios dentro de [scope]; o resto muda na hora, pelos eventos.
 */
class PrinterListViewModel(
    private val repository: PrinterRepository,
    private val historyRepository: QuoteHistoryRepository,
    private val maintenanceRepository: MaintenanceRepository,
    scope: CoroutineScope,
) {

    private val state = MutableStateFlow(PrintersUiState().withData())
    val uiState: StateFlow<PrintersUiState> = state.asStateFlow()

    private val noticeFlow = MutableSharedFlow<UiText>(extraBufferCapacity = NOTICE_BUFFER)
    val notices: SharedFlow<UiText> = noticeFlow.asSharedFlow()

    init {
        repository.printers.onEach { refresh() }.launchIn(scope)
        historyRepository.savedQuotes.onEach { refresh() }.launchIn(scope)
        maintenanceRepository.maintenance.onEach { refresh() }.launchIn(scope)
    }

    private fun refresh() {
        state.update { it.withData() }
    }

    fun startAdd() {
        state.update { it.copy(form = PrinterFormState.new()) }
    }

    /** Abre o formulário de nova impressora já preenchido com um preset, ver [PrinterPreset]. */
    fun startAddFromPreset(preset: PrinterPreset) {
        state.update {
            it.copy(
                form = PrinterFormState.new(name = "${preset.brand} ${preset.model}", powerWatts = preset.ratedPowerWatts, fromPreset = true),
                presetPickerOpen = false,
            )
        }
    }

    fun startEdit(id: String) {
        val printer = repository.printers.value.find { it.id == id } ?: return
        state.update { it.copy(form = printer.toFormState()) }
    }

    fun cancelEdit() {
        state.update { it.copy(form = null) }
    }

    fun updateForm(transform: (PrinterFormState) -> PrinterFormState) {
        state.update { s ->
            s.copy(
                form = s.form?.let {
                    transform(it).copy(
                        nameError = null,
                        powerError = null,
                        maintenanceError = null,
                        priceError = null,
                        paybackError = null,
                        daysError = null,
                        hoursError = null,
                    )
                },
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = state.value.form ?: return
        val name = current.name.trim()
        val power = parseDecimal(current.printerPowerWattsText.trim(), NumberKind.MEASURE)
        val maintenanceCost = parseDecimal(current.maintenanceCostPerHourText.trim())
        val price = parseDecimal(current.machinePriceText.trim())
        val payback = parseWholeNumber(current.paybackMonthsText.trim())
        val days = parseWholeNumber(current.printingDaysPerMonthText.trim())
        val hours = parseDecimal(current.printingHoursPerDayText.trim(), NumberKind.MEASURE)

        val nameError = uiText(Res.string.common_error_name_required).takeIf { name.isEmpty() }
        val powerError = nonNegativeError(current.printerPowerWattsText, power)
        val maintenanceError = nonNegativeError(current.maintenanceCostPerHourText, maintenanceCost)
        val priceError = nonNegativeError(current.machinePriceText, price)
        val paybackError = wholeError(current.paybackMonthsText, payback, max = null)
        val daysError = wholeError(current.printingDaysPerMonthText, days, max = MAX_DAYS_PER_MONTH)
        val hoursError = when {
            current.printingHoursPerDayText.isBlank() -> uiText(Res.string.common_error_required)
            hours == null -> uiText(Res.string.common_error_number_invalid)
            hours <= 0 -> uiText(Res.string.common_error_positive)
            hours > MAX_HOURS_PER_DAY -> uiText(Res.string.printers_error_hours_max)
            else -> null
        }
        if (listOf(nameError, powerError, maintenanceError, priceError, paybackError, daysError, hoursError).any { it != null } ||
            power == null || maintenanceCost == null || price == null || payback == null || days == null || hours == null
        ) {
            state.update {
                it.copy(
                    form = current.copy(
                        nameError = nameError,
                        powerError = powerError,
                        maintenanceError = maintenanceError,
                        priceError = priceError,
                        paybackError = paybackError,
                        daysError = daysError,
                        hoursError = hoursError,
                    ),
                )
            }
            return
        }

        val printer = PrinterProfile(
            id = current.id ?: Uuid.random().toString(),
            name = name,
            printerPowerWatts = power,
            maintenanceCostPerHour = maintenanceCost,
            machineInvestment = MachineInvestment(
                machinePrice = price,
                paybackMonths = payback,
                printingDaysPerMonth = days,
                printingHoursPerDay = hours,
            ),
        )
        if (current.id == null) repository.add(printer) else repository.updateKeepingArchived(printer)
        state.update { it.copy(form = null) }
        noticeFlow.tryEmit(uiText(Res.string.printers_notice_saved))
    }

    private fun nonNegativeError(text: String, value: Double?): UiText? = when {
        text.isBlank() -> uiText(Res.string.common_error_required)
        value == null -> uiText(Res.string.common_error_number_invalid)
        value < 0 -> uiText(Res.string.common_error_negative)
        else -> null
    }

    private fun wholeError(text: String, value: Int?, max: Int?): UiText? = when {
        text.isBlank() -> uiText(Res.string.common_error_required)
        value == null -> uiText(Res.string.common_error_whole_number)
        value <= 0 -> uiText(Res.string.common_error_positive)
        max != null && value > max -> uiText(Res.string.printers_error_days_max)
        else -> null
    }

    /** Pergunta se pode excluir a impressora [id], com quantos pedidos e produtos a usam (decisão 115). */
    fun requestDelete(id: String) {
        val printer = repository.printers.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = PendingDelete(id, printer.name, usageCount(id), canArchive = !printer.archived)) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    /** Quantos pedidos e produtos usam a impressora [id], inclusive os da lixeira. */
    fun usageCount(id: String): Int = CatalogUsage.printer(id, historyRepository.quotesIncludingTrash())

    /** Arquiva (ou restaura) a impressora [id], com a manutenção dela intacta (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        repository.setArchived(id, archived)
        state.update { it.copy(pendingDelete = null) }
        noticeFlow.tryEmit(uiText(if (archived) Res.string.printers_notice_archived else Res.string.printers_notice_restored))
    }

    fun delete(id: String) {
        repository.delete(id)
        maintenanceRepository.deleteAllFor(id)
        state.update { s ->
            s.copy(
                form = s.form?.takeUnless { it.id == id },
                pendingDelete = null,
                maintenance = s.maintenance?.takeUnless { it.printer.id == id },
            )
        }
        noticeFlow.tryEmit(uiText(Res.string.printers_notice_deleted))
    }

    fun openPresetPicker() {
        state.update { it.copy(presetPickerOpen = true) }
    }

    fun closePresetPicker() {
        state.update { it.copy(presetPickerOpen = false) }
    }

    // --- Manutenção (decisão 96) ---

    fun openMaintenance(printerId: String) {
        val printer = repository.printers.value.find { it.id == printerId } ?: return
        state.update { it.copy(maintenance = maintenanceDialogFor(printer)) }
    }

    fun closeMaintenance() {
        state.update { it.copy(maintenance = null) }
    }

    private fun currentHours(printerId: String) =
        MaintenanceReport.printerHours(printerId, historyRepository.savedQuotes.value, maintenanceRepository.maintenance.value.manualUsage)

    /**
     * Cadastra um componente. [hoursSinceText] vazio conta como zero (peça nova ou trocada agora).
     * Devolve o erro, ou `null` quando deu certo.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun addComponent(printerId: String, name: String, intervalText: String, hoursSinceText: String): UiText? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return uiText(Res.string.printers_error_component_name)
        val interval = parseDecimal(intervalText.trim(), NumberKind.MEASURE)?.takeIf { it > 0 } ?: return uiText(Res.string.printers_error_interval)
        val hoursSince = if (hoursSinceText.isBlank()) {
            0.0
        } else {
            parseDecimal(hoursSinceText.trim(), NumberKind.MEASURE)?.takeIf { it >= 0 } ?: return uiText(Res.string.printers_error_hours_since)
        }
        maintenanceRepository.addComponent(
            MaintenanceComponent(
                id = Uuid.random().toString(),
                printerId = printerId,
                name = trimmedName,
                intervalHours = interval,
                hoursAtLastService = MaintenanceReport.hoursAtLastServiceFor(currentHours(printerId), hoursSince),
            ),
        )
        return null
    }

    /** Muda nome e intervalo, sem mexer no contador. Devolve o erro, ou `null`. */
    fun updateComponent(component: MaintenanceComponent, name: String, intervalText: String): UiText? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return uiText(Res.string.printers_error_component_name)
        val interval = parseDecimal(intervalText.trim(), NumberKind.MEASURE)?.takeIf { it > 0 } ?: return uiText(Res.string.printers_error_interval)
        maintenanceRepository.updateComponent(component.copy(name = trimmedName, intervalHours = interval))
        return null
    }

    fun deleteComponent(id: String) = maintenanceRepository.deleteComponent(id)

    /** "Feito hoje": registra no diário com o nome do componente e recomeça o contador dele. */
    fun markServiced(component: MaintenanceComponent) {
        logService(component.printerId, todayEpochDay(), component.name, component.id)
    }

    /**
     * Registra uma manutenção no diário. Com [componentId], o contador daquele componente recomeça
     * agora, mesmo que a data informada seja antiga: o app não sabe quantas horas a máquina rodou
     * desde então. Devolve o erro, ou `null`.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun logService(printerId: String, dateEpochDay: Long, description: String, componentId: String?): UiText? {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return uiText(Res.string.printers_error_log_description)
        maintenanceRepository.logService(
            MaintenanceLogEntry(
                id = Uuid.random().toString(),
                printerId = printerId,
                dateEpochDay = dateEpochDay,
                description = trimmed,
                componentId = componentId,
            ),
            printerHoursNow = currentHours(printerId),
        )
        return null
    }

    fun deleteLogEntry(id: String) = maintenanceRepository.deleteLogEntry(id)

    /** Lança horas de uso fora de orçamento, com a data de hoje. Devolve o erro, ou `null`. */
    @OptIn(ExperimentalUuidApi::class)
    fun addManualUsage(printerId: String, hoursText: String, reason: String): UiText? {
        val hours = parseDecimal(hoursText.trim(), NumberKind.MEASURE)?.takeIf { it > 0 } ?: return uiText(Res.string.printers_error_usage_hours)
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return uiText(Res.string.printers_error_usage_reason)
        maintenanceRepository.addManualUsage(
            ManualUsageEntry(
                id = Uuid.random().toString(),
                printerId = printerId,
                dateEpochDay = todayEpochDay(),
                hours = hours,
                reason = trimmed,
            ),
        )
        return null
    }

    fun deleteManualUsage(id: String) = maintenanceRepository.deleteManualUsage(id)

    // --- Montagem do estado ---

    private fun PrintersUiState.withData(): PrintersUiState {
        val printers = repository.printers.value
        val quotes = historyRepository.savedQuotes.value
        val allMaintenance = maintenanceRepository.maintenance.value
        val queueByPrinter = PrintQueueReport.summarize(printers, quotes).associateBy { it.printer.id }
        val rows = printers.map { printer ->
            val statuses = statusesOf(printer.id, quotes, allMaintenance)
            val queue = queueByPrinter[printer.id]
            PrinterRowState(
                printer = printer,
                machineCostPerHour = printer.machineCostPerHour,
                queuedMinutes = queue?.queuedMinutes ?: 0.0,
                queuedQuoteCount = queue?.queuedQuoteCount ?: 0,
                maintenance = summaryOf(statuses),
            )
        }
        val (archived, active) = rows.partition { it.printer.archived }
        val dialogPrinter = maintenance?.let { open -> printers.find { it.id == open.printer.id } }
        return copy(
            active = active,
            archived = archived,
            maintenance = dialogPrinter?.let { maintenanceDialogFor(it, quotes, allMaintenance) },
        )
    }

    private fun maintenanceDialogFor(
        printer: PrinterProfile,
        quotes: List<SavedQuote> = historyRepository.savedQuotes.value,
        all: PrinterMaintenance = maintenanceRepository.maintenance.value,
    ) = MaintenanceDialogState(
        printer = printer,
        hours = MaintenanceReport.printerHours(printer.id, quotes, all.manualUsage),
        statuses = statusesOf(printer.id, quotes, all),
        data = all.forPrinter(printer.id),
    )

    /** Situação de cada componente de [printerId], o mais urgente primeiro. */
    private fun statusesOf(printerId: String, quotes: List<SavedQuote>, all: PrinterMaintenance): List<ComponentStatus> {
        val hours = MaintenanceReport.printerHours(printerId, quotes, all.manualUsage)
        return all.components
            .filter { it.printerId == printerId }
            .map { MaintenanceReport.componentStatus(it, hours) }
            .sortedBy { it.remainingHours }
    }

    private fun summaryOf(statuses: List<ComponentStatus>): MaintenanceSummary? {
        val urgent = MaintenanceReport.mostUrgent(statuses) ?: return null
        val overdue = statuses.count { it.state == MaintenanceState.OVERDUE }
        return MaintenanceSummary(
            componentName = urgent.component.name,
            state = urgent.state,
            remainingHours = urgent.remainingHours,
            otherOverdue = overdue - if (urgent.state == MaintenanceState.OVERDUE) 1 else 0,
        )
    }

    private companion object {
        const val NOTICE_BUFFER = 8
        const val MAX_DAYS_PER_MONTH = 31
        const val MAX_HOURS_PER_DAY = 24
    }
}
