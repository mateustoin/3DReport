package com.threedreport.app.ui.printers

import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.app.ui.format.toRequiredInt
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela de Impressoras: lista os perfis salvos e edita um por vez
 * em [form] (nulo quando nenhum formulário está aberto).
 */
class PrinterListViewModel(private val repository: PrinterRepository) {

    val printers: StateFlow<List<PrinterProfile>> = repository.printers

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
        if (formState.value?.id == id) formState.value = null
    }
}
