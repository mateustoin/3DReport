package com.threedreport.app.ui.settings

import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da tela de Configurações.
 *
 * Edição em rascunho: as mudanças só valem para o resto do app depois de
 * [save], que valida os campos e grava em [SettingsRepository].
 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) {

    private val state = MutableStateFlow(settingsRepository.settings.value.toUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    fun update(transform: (SettingsUiState) -> SettingsUiState) {
        state.value = transform(state.value).copy(savedConfirmation = false)
    }

    fun save() {
        val current = state.value
        val settings = runCatching {
            PricingSettings(
                energyPricePerKwh = current.energyPricePerKwhText.toRequiredDouble("Preço do kWh"),
                printerPowerWatts = current.printerPowerWattsText.toRequiredDouble("Consumo da impressora"),
                maintenanceCostPerHour = current.maintenanceCostPerHourText.toRequiredDouble("Manutenção por hora"),
                failureRate = current.failureRatePercentText.toRequiredDouble("Taxa de falhas") / 100.0,
                finishingRate = current.finishingRatePercentText.toRequiredDouble("Taxa de acabamento") / 100.0,
                administrativeCost = current.administrativeCostText.toRequiredDouble("Custo administrativo"),
                machineInvestment = MachineInvestment(
                    machinePrice = current.machinePriceText.toRequiredDouble("Valor da máquina"),
                    paybackMonths = current.paybackMonthsText.trim().toIntOrNull()
                        ?: error("Prazo de retorno (meses) inválido"),
                    printingDaysPerMonth = current.printingDaysPerMonthText.trim().toIntOrNull()
                        ?: error("Dias de uso por mês inválido"),
                    printingHoursPerDay = current.printingHoursPerDayText.toRequiredDouble("Horas de uso por dia"),
                ),
                profitMargin = current.profitMarginPercentText.toRequiredDouble("Margem de lucro") / 100.0,
            )
        }

        state.value = settings.fold(
            onSuccess = {
                settingsRepository.update(it)
                current.copy(errorMessage = null, savedConfirmation = true)
            },
            onFailure = { current.copy(errorMessage = it.message, savedConfirmation = false) },
        )
    }

    private fun String.toRequiredDouble(fieldLabel: String): Double =
        parseDecimal(this) ?: error("$fieldLabel inválido")
}

private fun PricingSettings.toUiState() = SettingsUiState(
    energyPricePerKwhText = energyPricePerKwh.toString(),
    printerPowerWattsText = printerPowerWatts.toString(),
    maintenanceCostPerHourText = maintenanceCostPerHour.toString(),
    failureRatePercentText = (failureRate * 100).toString(),
    finishingRatePercentText = (finishingRate * 100).toString(),
    administrativeCostText = administrativeCost.toString(),
    machinePriceText = machineInvestment.machinePrice.toString(),
    paybackMonthsText = machineInvestment.paybackMonths.toString(),
    printingDaysPerMonthText = machineInvestment.printingDaysPerMonth.toString(),
    printingHoursPerDayText = machineInvestment.printingHoursPerDay.toString(),
    profitMarginPercentText = (profitMargin * 100).toString(),
)
