package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de canais de venda (ver [CatalogRepository]). */
interface SalesChannelRepository : CatalogRepository<SalesChannel> {
    val channels: StateFlow<List<SalesChannel>>
        get() = items
}

class RecordSalesChannelRepository(collection: RecordCollection<SalesChannel>) :
    RecordCatalogRepository<SalesChannel>(collection), SalesChannelRepository
