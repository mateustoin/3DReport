package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import kotlinx.coroutines.flow.StateFlow

/**
 * Se o onboarding da primeira execução já foi concluído ou pulado. Arquivo próprio (decisão 81): é
 * estado desta instalação, não parâmetro de custo.
 */
interface OnboardingRepository {
    val completed: StateFlow<Boolean>

    fun markCompleted()
}

class StoredOnboardingRepository(private val document: DocumentValue<Boolean>) : OnboardingRepository {
    override val completed: StateFlow<Boolean> = document.value

    override fun markCompleted() = document.set(true)
}
