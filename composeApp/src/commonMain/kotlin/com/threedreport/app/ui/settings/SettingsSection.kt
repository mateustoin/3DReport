package com.threedreport.app.ui.settings

/** As seções de Configurações, uma por vez na tela (decisão 112). */
enum class SettingsSection {
    BUSINESS,
    CHANNELS,
    DOCUMENTS,
    APPEARANCE,
    BACKUP,
}

/** Se Configurações tem rascunho por salvar (custos ou documentos), pra barra lateral marcar. */
fun hasUnsavedSettings(viewModel: SettingsViewModel, brandingViewModel: BrandingViewModel): Boolean =
    viewModel.hasUnsavedChanges || brandingViewModel.hasUnsavedChanges

/**
 * Salva o que está em rascunho nos custos e nos documentos de uma vez, com um aviso só. Devolve a seção do primeiro erro, pra
 * a tela levar a pessoa até ela: com a barra embaixo, a mensagem ficaria numa seção que não está na tela. `null` quando
 * tudo foi salvo (ou não havia nada a salvar).
 */
fun saveAllSettings(viewModel: SettingsViewModel, brandingViewModel: BrandingViewModel): SettingsSection? {
    val pricingDirty = viewModel.hasUnsavedChanges
    val brandingDirty = brandingViewModel.hasUnsavedChanges
    if (pricingDirty) viewModel.save()
    // Um aviso só, mesmo salvando custos e documentos juntos: os custos já avisaram, a não ser que tenham falhado.
    val pricingFailed = viewModel.uiState.value.errors.isNotEmpty()
    if (brandingDirty) brandingViewModel.save(announce = !pricingDirty || pricingFailed)
    return when {
        viewModel.uiState.value.errors.isNotEmpty() -> SettingsSection.BUSINESS
        brandingViewModel.uiState.value.let { it.validityError != null || it.optionsError != null } -> SettingsSection.DOCUMENTS
        else -> null
    }
}
