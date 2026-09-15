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

    private val state = MutableStateFlow(
        BrandingUiState(watermarkTextInput = repository.branding.value.watermarkText.orEmpty())
    )
    val uiState: StateFlow<BrandingUiState> = state.asStateFlow()

    fun update(text: String) {
        state.value = state.value.copy(watermarkTextInput = text, savedConfirmation = false)
    }

    fun save() {
        repository.update(BrandingSettings(watermarkText = state.value.watermarkTextInput.trim().ifEmpty { null }))
        state.value = state.value.copy(savedConfirmation = true)
    }
}
