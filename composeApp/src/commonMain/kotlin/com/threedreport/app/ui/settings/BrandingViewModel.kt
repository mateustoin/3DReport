package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da seção de marca d'água (parte da tela de Configurações, mas
 * separado de [SettingsViewModel] porque não é um parâmetro de custo — ver
 * [BrandingRepository]). Também cobre "Salvar como template"
 * ([confirmSaveAsTemplate]), que tira uma foto do que está no formulário
 * (mesmos campos de [uiState], mesmo que ainda não tenham sido persistidos
 * com [save]) e guarda como um novo [QuoteTemplate] — ver aba Templates.
 */
class BrandingViewModel(
    private val repository: BrandingRepository,
    private val templateRepository: TemplateRepository,
) {

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
        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                repository.update(settings)
                state.value = current.copy(errorMessage = null, savedConfirmation = true)
            },
            onFailure = { state.value = current.copy(errorMessage = it.message, savedConfirmation = false) },
        )
    }

    fun startSaveAsTemplate() {
        state.value = state.value.copy(
            isSavingAsTemplate = true,
            templateNameInput = "",
            templateSaveError = null,
            templateSavedConfirmation = false,
        )
    }

    fun updateTemplateName(text: String) {
        state.value = state.value.copy(templateNameInput = text, templateSaveError = null)
    }

    fun cancelSaveAsTemplate() {
        state.value = state.value.copy(isSavingAsTemplate = false, templateNameInput = "", templateSaveError = null)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun confirmSaveAsTemplate() {
        val current = state.value
        val name = current.templateNameInput.trim()
        if (name.isBlank()) {
            state.value = current.copy(templateSaveError = "Nome não pode ser vazio")
            return
        }

        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                templateRepository.add(
                    QuoteTemplate(
                        id = Uuid.random().toString(),
                        name = name,
                        watermarkText = settings.watermarkText,
                        showWatermark = settings.showWatermark,
                        showFooter = settings.showFooter,
                    )
                )
                state.value = current.copy(
                    isSavingAsTemplate = false,
                    templateNameInput = "",
                    templateSaveError = null,
                    templateSavedConfirmation = true,
                )
            },
            onFailure = { state.value = current.copy(templateSaveError = it.message) },
        )
    }
}

private fun BrandingUiState.toSettingsOrError(): Result<BrandingSettings> {
    val watermarkText = watermarkTextInput.trim().ifEmpty { null }
    if (watermarkText != null && !showWatermark && !showFooter) {
        return Result.failure(IllegalStateException("Selecione ao menos uma opção: marca d'água ou rodapé."))
    }
    return Result.success(BrandingSettings(watermarkText = watermarkText, showWatermark = showWatermark, showFooter = showFooter))
}

private fun BrandingSettings.toUiState() = BrandingUiState(
    watermarkTextInput = watermarkText.orEmpty(),
    showWatermark = showWatermark,
    showFooter = showFooter,
)
