package com.threedreport.app.ui.templates

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/** Um modelo na lista, e se ele é o que está valendo agora ([isActive]). */
data class TemplateRowState(val template: QuoteTemplate, val isActive: Boolean)

/** Tudo o que o diálogo de modelos mostra: a lista e a pergunta de exclusão pendente (nula quando nenhuma). */
data class TemplatesUiState(
    val rows: List<TemplateRowState> = emptyList(),
    val pendingDelete: QuoteTemplate? = null,
)

/**
 * ViewModel do diálogo de Modelos: biblioteca de "fotos" nomeadas do
 * [BrandingSettings] ativo. Não tem formulário próprio de edição (esse
 * fica em Configurações, Documentos pro cliente, ver [com.threedreport.app.ui.settings.BrandingViewModel]).
 * Um [uiState] só; a lista acompanha os repositórios dentro de [scope].
 * Carregar um modelo não é aqui: quem carrega é o rascunho de Documentos
 * ([com.threedreport.app.ui.settings.BrandingViewModel.applyTemplate]), pra a pessoa salvar ou descartar.
 * O [brandingRepository] só serve pra marcar qual modelo está valendo.
 */
class TemplateListViewModel(
    private val repository: TemplateRepository,
    private val brandingRepository: BrandingRepository,
    scope: CoroutineScope,
) {

    private val state = MutableStateFlow(rowsOf(repository.templates.value, brandingRepository.branding.value))
    val uiState: StateFlow<TemplatesUiState> = state.asStateFlow()

    init {
        combine(repository.templates, brandingRepository.branding) { templates, branding -> templates to branding }
            .onEach { (templates, branding) -> state.update { it.copy(rows = rowsOf(templates, branding).rows) } }
            .launchIn(scope)
    }

    private fun rowsOf(templates: List<QuoteTemplate>, branding: BrandingSettings): TemplatesUiState {
        val activeId = activeTemplateId(templates, branding)
        return TemplatesUiState(rows = templates.map { TemplateRowState(it, isActive = it.id == activeId) })
    }

    /**
     * Função pura: id do template (se algum) cujos campos batem com [branding], a config ativa.
     * Compara só os campos de template: logo e contato são identidade e não entram na conta.
     */
    fun activeTemplateId(templates: List<QuoteTemplate>, branding: BrandingSettings): String? =
        templates.firstOrNull {
            it.brandName == branding.brandName &&
                it.showWatermark == branding.showWatermark &&
                it.showFooter == branding.showFooter &&
                it.showPrintTime == branding.showPrintTime &&
                it.showBorder == branding.showBorder
        }?.id

    /** Pergunta se pode excluir o template [id]. */
    fun requestDelete(id: String) {
        val template = repository.templates.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = template) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    fun delete(id: String) {
        repository.delete(id)
        state.update { it.copy(pendingDelete = null) }
    }
}
