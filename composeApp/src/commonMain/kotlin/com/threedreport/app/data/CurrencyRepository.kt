package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.core.model.Currency
import kotlinx.coroutines.flow.StateFlow

/**
 * Moeda padrão dos orçamentos **novos** (decisão 106). Cada orçamento salvo guarda a própria moeda
 * ([com.threedreport.core.model.SavedQuote.currency]), então trocar aqui não re-rotula o histórico.
 */
interface CurrencyRepository : DocumentRepository<Currency> {
    val currency: StateFlow<Currency>
        get() = value
}

class StoredCurrencyRepository(document: DocumentValue<Currency>) :
    StoredDocumentRepository<Currency>(document), CurrencyRepository
