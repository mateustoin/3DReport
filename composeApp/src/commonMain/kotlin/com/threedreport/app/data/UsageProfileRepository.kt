package com.threedreport.app.data

import com.threedreport.core.model.UsageProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda o [UsageProfile] (decisão 103), começando em [UsageProfile.SELLER]: quem já usava o app
 * antes da pergunta existir continua vendo tudo exatamente como antes. Arquivo próprio pelo mesmo
 * motivo do [OnboardingRepository]: é estado da interface, não parâmetro de custo.
 */
expect class UsageProfileRepository() {
    val profile: StateFlow<UsageProfile>
    fun update(profile: UsageProfile)
}
