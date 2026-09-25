package com.threedreport.app.ui.templates

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel da tela de Templates: biblioteca de "fotos" nomeadas do
 * [BrandingSettings] ativo — não tem formulário próprio de edição (esse
 * fica em Configurações → Marca d'água do PDF, ver [com.threedreport.app.ui.settings.BrandingViewModel]).
 * [load] copia um preset de volta pra dentro do [BrandingRepository] ativo.
 */
class TemplateListViewModel(
    private val repository: TemplateRepository,
    private val brandingRepository: BrandingRepository,
) {

    val templates: StateFlow<List<QuoteTemplate>> = repository.templates
    val activeBranding: StateFlow<BrandingSettings> = brandingRepository.branding

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

    fun delete(id: String) = repository.delete(id)

    /**
     * "Carregar": copia o preset [id] pra dentro do [BrandingRepository] ativo. Parte da
     * configuração atual, e não de uma em branco, pra logo e contato continuarem onde estão.
     */
    fun load(id: String) {
        val template = repository.templates.value.find { it.id == id } ?: return
        brandingRepository.update(
            brandingRepository.branding.value.copy(
                brandName = template.brandName,
                showWatermark = template.showWatermark,
                showFooter = template.showFooter,
                showPrintTime = template.showPrintTime,
                showBorder = template.showBorder,
            )
        )
    }
}
