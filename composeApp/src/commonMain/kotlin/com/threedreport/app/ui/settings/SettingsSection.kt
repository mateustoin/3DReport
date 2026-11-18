package com.threedreport.app.ui.settings

/** As seções de Configurações, uma por vez na tela (decisão 112). */
enum class SettingsSection {
    BUSINESS,
    CHANNELS,
    ORDERS,
    DOCUMENTS,
    APPEARANCE,
    BACKUP,
}

/** Se Configurações tem rascunho por salvar (custos, pedidos ou documentos), pra barra lateral marcar. */
fun hasUnsavedSettings(viewModel: SettingsViewModel, brandingViewModel: BrandingViewModel, orderSettingsViewModel: OrderSettingsViewModel? = null): Boolean =
    viewModel.hasUnsavedChanges || brandingViewModel.hasUnsavedChanges || orderSettingsViewModel?.hasUnsavedChanges == true

/**
 * Salva o que está em rascunho nos custos, nos pedidos e nos documentos de uma vez, com um aviso só. Valida todos antes
 * de gravar: com erro em qualquer um, nenhum grava e não há aviso de salvo. Devolve a seção do primeiro erro, pra a tela
 * levar a pessoa até ela: com a barra embaixo, a mensagem ficaria numa seção que não está na tela. `null` quando tudo foi
 * salvo (ou não havia nada a salvar).
 */
fun saveAllSettings(viewModel: SettingsViewModel, brandingViewModel: BrandingViewModel, orderSettingsViewModel: OrderSettingsViewModel? = null): SettingsSection? {
    val pricingDirty = viewModel.hasUnsavedChanges
    val brandingDirty = brandingViewModel.hasUnsavedChanges
    val ordersDirty = orderSettingsViewModel?.hasUnsavedChanges == true
    // As validações rodam sempre, pra cada seção mostrar os seus erros mesmo quando outra também falhou.
    val pricingValid = !pricingDirty || viewModel.validate()
    val ordersValid = !ordersDirty || orderSettingsViewModel!!.validate()
    val brandingValid = !brandingDirty || brandingViewModel.validate()
    if (!pricingValid) return SettingsSection.BUSINESS
    if (!ordersValid) return SettingsSection.ORDERS
    if (!brandingValid) return SettingsSection.DOCUMENTS
    // Um aviso só: o dos custos, quando eles são salvos; senão o dos pedidos; senão o dos documentos.
    if (pricingDirty) viewModel.save()
    if (ordersDirty) orderSettingsViewModel!!.save(announce = !pricingDirty)
    if (brandingDirty) brandingViewModel.save(announce = !pricingDirty && !ordersDirty)
    return null
}
