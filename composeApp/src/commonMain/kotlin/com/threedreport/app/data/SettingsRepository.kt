package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.StateFlow

/** Parâmetros de custo do negócio (ver [DocumentRepository]). */
interface SettingsRepository : DocumentRepository<PricingSettings> {
    val settings: StateFlow<PricingSettings>
        get() = value
}

class StoredSettingsRepository(document: DocumentValue<PricingSettings>) :
    StoredDocumentRepository<PricingSettings>(document), SettingsRepository
