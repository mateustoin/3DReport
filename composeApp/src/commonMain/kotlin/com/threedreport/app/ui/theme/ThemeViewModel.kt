package com.threedreport.app.ui.theme

import com.threedreport.app.data.AppLanguage
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.ThemeRepository
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel da aparência: tema e idioma. Diferente de [com.threedreport.app.ui.settings.SettingsViewModel], não tem
 * rascunho nem "Salvar": trocar de tema ou de idioma vale na hora (decisões 35 e 152).
 */
class ThemeViewModel(
    private val repository: ThemeRepository,
    private val preferences: PreferencesRepository,
    scope: CoroutineScope,
) {
    val mode: StateFlow<ThemeMode> = repository.mode

    /** O idioma escolhido (automático, português ou inglês). Quem aplica na plataforma acompanha as preferências. */
    val language: StateFlow<AppLanguage> =
        preferences.preferences.map { it.language }.stateIn(scope, SharingStarted.Eagerly, preferences.preferences.value.language)

    fun setMode(mode: ThemeMode) = repository.update(mode)

    fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)
}
