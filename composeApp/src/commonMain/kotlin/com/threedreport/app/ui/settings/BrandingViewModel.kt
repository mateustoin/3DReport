package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel da seção de marca d'água (parte da tela de Configurações, mas
 * separado de [SettingsViewModel] porque não é um parâmetro de custo — ver
 * [BrandingRepository]).
 */
class BrandingViewModel(private val repository: BrandingRepository) {

    private val state = MutableStateFlow(repository.branding.value.toUiState())
    val uiState: StateFlow<BrandingUiState> = state.asStateFlow()

    fun update(text: String) {
        state.value = state.value.copy(watermarkTextInput = text, errorMessage = null, savedConfirmation = false)
    }

    fun setShowWatermark(show: Boolean) {
        state.value = state.value.copy(showWatermark = show, errorMessage = null, savedConfirmation = false)
    }

    fun setShowFooter(show: Boolean) {
        state.value = state.value.copy(showFooter = show, errorMessage = null, savedConfirmation = false)
    }

    fun save() {
        val current = state.value
        val watermarkText = current.watermarkTextInput.trim().ifEmpty { null }

        if (watermarkText != null && !current.showWatermark && !current.showFooter) {
            state.value = current.copy(errorMessage = "Selecione ao menos uma opção: marca d'água ou rodapé.")
            return
        }

        repository.update(
            BrandingSettings(
                watermarkText = watermarkText,
                showWatermark = current.showWatermark,
                showFooter = current.showFooter,
            )
        )
        state.value = current.copy(errorMessage = null, savedConfirmation = true)
    }
}

private fun BrandingSettings.toUiState() = BrandingUiState(
    watermarkTextInput = watermarkText.orEmpty(),
    showWatermark = showWatermark,
    showFooter = showFooter,
)
