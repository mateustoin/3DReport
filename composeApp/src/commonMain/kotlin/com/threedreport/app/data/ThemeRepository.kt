package com.threedreport.app.data

import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda a preferência de tema do app ([ThemeMode]). A implementação
 * persiste em disco (ver `actual` na fonte de cada plataforma), começando
 * em [ThemeMode.SYSTEM].
 */
expect class ThemeRepository() {
    val mode: StateFlow<ThemeMode>
    fun update(mode: ThemeMode)
}
