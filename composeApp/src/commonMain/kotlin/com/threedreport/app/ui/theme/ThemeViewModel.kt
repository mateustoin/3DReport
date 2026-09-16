package com.threedreport.app.ui.theme

import com.threedreport.app.data.ThemeRepository
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel da preferência de tema. Diferente de [com.threedreport.app.ui.settings.SettingsViewModel],
 * não tem rascunho/"Salvar" — trocar de tema é uma ação instantânea.
 */
class ThemeViewModel(private val repository: ThemeRepository) {
    val mode: StateFlow<ThemeMode> = repository.mode

    fun setMode(mode: ThemeMode) = repository.update(mode)
}
