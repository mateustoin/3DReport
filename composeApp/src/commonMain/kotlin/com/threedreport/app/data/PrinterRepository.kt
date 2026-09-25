package com.threedreport.app.data

import com.threedreport.app.data.store.RecordCollection
import com.threedreport.core.model.PrinterProfile
import kotlinx.coroutines.flow.StateFlow

/** Cadastro de impressoras (ver [CatalogRepository]). */
interface PrinterRepository : CatalogRepository<PrinterProfile> {
    val printers: StateFlow<List<PrinterProfile>>
        get() = items
}

class RecordPrinterRepository(collection: RecordCollection<PrinterProfile>) :
    RecordCatalogRepository<PrinterProfile>(collection), PrinterRepository
