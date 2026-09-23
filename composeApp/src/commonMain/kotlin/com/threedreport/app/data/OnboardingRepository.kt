package com.threedreport.app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Lembra se a pessoa já passou pelas perguntas iniciais, pra elas aparecerem uma vez só.
 *
 * Fica num arquivo próprio em vez de um campo em `PricingSettings` porque não é parâmetro de custo:
 * é estado da interface, e misturar os dois faria um backup de configurações carregar junto o
 * "já vi isso" de outra máquina.
 */
expect class OnboardingRepository() {
    val completed: StateFlow<Boolean>
    fun markCompleted()
}
