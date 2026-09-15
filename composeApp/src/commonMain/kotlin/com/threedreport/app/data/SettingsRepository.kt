package com.threedreport.app.data

import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda os parâmetros de custo do negócio ([PricingSettings]), compartilhados
 * entre a tela de Configurações (que os edita) e a tela de Orçamento (que os
 * lê para calcular o resultado).
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), pré-carregada com valores padrão no primeiro uso.
 */
expect class SettingsRepository() {
    val settings: StateFlow<PricingSettings>
    fun update(settings: PricingSettings)
}
