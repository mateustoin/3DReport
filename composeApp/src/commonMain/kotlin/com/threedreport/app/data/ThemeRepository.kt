package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Tema claro/escuro. Preferência deste computador, não do negócio (não iria pra nuvem). */
interface ThemeRepository : DocumentRepository<ThemeMode> {
    val mode: StateFlow<ThemeMode>
        get() = value
}

class StoredThemeRepository(document: DocumentValue<ThemeMode>) :
    StoredDocumentRepository<ThemeMode>(document), ThemeRepository
