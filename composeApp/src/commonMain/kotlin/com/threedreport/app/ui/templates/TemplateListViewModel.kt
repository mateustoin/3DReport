package com.threedreport.app.ui.templates

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
 * ViewModel da tela de Templates: lista o catálogo e edita um item por vez
 * em [form] (nulo quando nenhum formulário está aberto). "Usar este"
 * ([applyToActive]) copia o preset pra dentro do [BrandingRepository] ativo
 * — o mesmo usado por todos os exports em PDF (ver Configurações → Marca
 * d'água do PDF, que continua sendo o editor da config em uso).
 */
class TemplateListViewModel(
    private val repository: TemplateRepository,
    private val brandingRepository: BrandingRepository,
) {

    val templates: StateFlow<List<QuoteTemplate>> = repository.templates

    private val formState = MutableStateFlow<TemplateFormState?>(null)
    val form: StateFlow<TemplateFormState?> = formState.asStateFlow()

    fun startAdd() {
        formState.value = TemplateFormState()
    }

    fun startEdit(template: QuoteTemplate) {
        formState.value = template.toFormState()
    }

    fun cancelEdit() {
        formState.value = null
    }

    fun updateForm(transform: (TemplateFormState) -> TemplateFormState) {
        formState.value = formState.value?.let { transform(it).copy(errorMessage = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value ?: return
        val result = runCatching {
            QuoteTemplate(
                id = current.id ?: Uuid.random().toString(),
                name = current.name.trim().ifEmpty { error("Nome não pode ser vazio") },
                watermarkText = current.watermarkText.trim().ifEmpty { null },
                showWatermark = current.showWatermark,
                showFooter = current.showFooter,
            )
        }

        result.fold(
            onSuccess = { template ->
                if (current.id == null) repository.add(template) else repository.update(template)
                formState.value = null
            },
            onFailure = { formState.value = current.copy(errorMessage = it.message) },
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value?.id == id) formState.value = null
    }

    /** "Usar este": copia o preset [id] pra dentro do [BrandingRepository] ativo. */
    fun applyToActive(id: String) {
        val template = repository.templates.value.find { it.id == id } ?: return
        brandingRepository.update(
            BrandingSettings(
                watermarkText = template.watermarkText,
                showWatermark = template.showWatermark,
                showFooter = template.showFooter,
            )
        )
    }
}
