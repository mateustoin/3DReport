package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.Service
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de serviços (ver [CatalogRepository]). */
interface ServiceRepository : CatalogRepository<Service> {
    val services: StateFlow<List<Service>>
        get() = items
}

class RecordServiceRepository(collection: RecordCollection<Service>) :
    RecordCatalogRepository<Service>(collection), ServiceRepository
