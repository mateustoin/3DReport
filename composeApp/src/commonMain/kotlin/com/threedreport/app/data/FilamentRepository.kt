package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.Filament
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de filamentos (ver [CatalogRepository]). */
interface FilamentRepository : CatalogRepository<Filament> {
    val filaments: StateFlow<List<Filament>>
        get() = items
}

class RecordFilamentRepository(collection: RecordCollection<Filament>) :
    RecordCatalogRepository<Filament>(collection), FilamentRepository
