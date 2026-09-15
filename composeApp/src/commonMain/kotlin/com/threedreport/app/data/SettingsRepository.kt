package com.threedreport.app.data

import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda os parâmetros de custo da operação ([PricingSettings]), compartilhados
 * entre a tela de Configurações (que os edita) e a tela de Orçamento (que os lê
 * para calcular o resultado).
 *
 * Implementação atual mantém o valor apenas em memória, com valores padrão
 * pré-carregados; persistência real fica para uma decisão futura (ver
 * "Pendentes de aprovação" em docs/decisions.md).
 */
class SettingsRepository {

    private val state = MutableStateFlow(DEFAULT_SETTINGS)
    val settings: StateFlow<PricingSettings> = state.asStateFlow()

    fun update(settings: PricingSettings) {
        state.value = settings
    }

    companion object {
        val DEFAULT_SETTINGS = PricingSettings(
            energyPricePerKwh = 1.23,
            printerPowerWatts = 380.0,
            maintenanceCostPerHour = 0.17,
            failureRate = 0.10,
            finishingRate = 0.10,
            administrativeCost = 0.0,
            machineInvestment = MachineInvestment(
                machinePrice = 2700.0,
                paybackMonths = 12,
                printingDaysPerMonth = 25,
                printingHoursPerDay = 16.0,
            ),
            profitMargin = 1.0,
        )
    }
}
