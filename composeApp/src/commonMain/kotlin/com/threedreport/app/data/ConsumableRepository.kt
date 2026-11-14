package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.Consumable
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de insumos (argola, ímã, caixa), decisão 122 (ver [CatalogRepository]). Nasce vazio. */
interface ConsumableRepository : CatalogRepository<Consumable> {
    val consumables: StateFlow<List<Consumable>>
        get() = items
}

class RecordConsumableRepository(collection: RecordCollection<Consumable>) :
    RecordCatalogRepository<Consumable>(collection), ConsumableRepository
