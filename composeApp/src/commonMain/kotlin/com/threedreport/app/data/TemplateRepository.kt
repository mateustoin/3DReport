package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de templates de documento (ver [CatalogRepository]). */
interface TemplateRepository : CatalogRepository<QuoteTemplate> {
    val templates: StateFlow<List<QuoteTemplate>>
        get() = items
}

class RecordTemplateRepository(collection: RecordCollection<QuoteTemplate>) :
    RecordCatalogRepository<QuoteTemplate>(collection), TemplateRepository
