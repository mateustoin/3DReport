package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.core.model.UsageProfile
import kotlinx.coroutines.flow.StateFlow

/** Perfil de uso (decisão 103): só define o que vem escolhido. */
interface UsageProfileRepository : DocumentRepository<UsageProfile> {
    val profile: StateFlow<UsageProfile>
        get() = value
}

class StoredUsageProfileRepository(document: DocumentValue<UsageProfile>) :
    StoredDocumentRepository<UsageProfile>(document), UsageProfileRepository
