package com.threedreport.app.ui.settings

import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da tela de Configurações gerais.
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
                failureRate = current.failureRatePercentText.toRequiredDouble("Taxa de falhas") / 100.0,
                finishingRate = current.finishingRatePercentText.toRequiredDouble("Taxa de acabamento") / 100.0,
                administrativeCost = current.administrativeCostText.toRequiredDouble("Custo administrativo"),
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
}

private fun PricingSettings.toUiState() = SettingsUiState(
    energyPricePerKwhText = energyPricePerKwh.toString(),
    failureRatePercentText = (failureRate * 100).toString(),
    finishingRatePercentText = (finishingRate * 100).toString(),
    administrativeCostText = administrativeCost.toString(),
    profitMarginPercentText = (profitMargin * 100).toString(),
)
